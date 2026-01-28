package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.exceptions.AlgorithmNotFoundException;
import pl.edu.agh.to.kotospring.server.exceptions.ProblemNotFoundException;
import pl.edu.agh.to.kotospring.server.models.GroupIndicatorAggregateRow;
import pl.edu.agh.to.kotospring.server.models.IndicatorAggregateRow;
import pl.edu.agh.to.kotospring.server.models.PartStatusInfo;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.*;
import pl.edu.agh.to.kotospring.server.services.interfaces.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExperimentServiceImpl implements ExperimentService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentServiceImpl.class);

    private final ProblemRegistryService problemRegistry;
    private final AlgorithmRegistryService algorithmRegistry;
    private final IndicatorRegistryService indicatorRegistry;
    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentPartExecutionRepository experimentPartExecutionRepository;
    private final ExperimentExecutionService executionService;
    private final ExperimentRepository experimentRepository;
    private final ExperimentAggregateRepository experimentAggregateRepository;
    private final ExperimentGroupRepository experimentGroupRepository;

    public ExperimentServiceImpl(ProblemRegistryService problemRegistry,
            AlgorithmRegistryService algorithmRegistry,
            IndicatorRegistryService indicatorRegistry,
            ExperimentRunRepository experimentRunRepository,
            ExperimentPartRepository experimentPartRepository,
            ExperimentPartExecutionRepository experimentPartExecutionRepository,
            ExperimentExecutionService executionService,
            ExperimentRepository experimentRepository,
            ExperimentAggregateRepository experimentAggregateRepository,
            ExperimentGroupRepository experimentGroupRepository) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
        this.experimentRunRepository = experimentRunRepository;
        this.experimentPartRepository = experimentPartRepository;
        this.experimentPartExecutionRepository = experimentPartExecutionRepository;
        this.executionService = executionService;
        this.experimentRepository = experimentRepository;
        this.experimentAggregateRepository = experimentAggregateRepository;
        this.experimentGroupRepository = experimentGroupRepository;
    }

    @Override
    @Transactional
    public Experiment createExperiment(CreateExperimentRequest request) {
        logger.info("Received request to create new full experiment with {} runs, {} parts each", request.runCount(),
                request.parts().size());
        Experiment experiment = new Experiment(OffsetDateTime.now(), request.runCount());
        experimentRepository.saveAndFlush(experiment);

        List<Pair<ExperimentPart, CreateExperimentRequestData>> definitions = request.parts().stream()
                .map(this::createExperimentPartDefinition)
                .toList();

        definitions.forEach(pair -> experiment.addPart(pair.getFirst()));
        experimentPartRepository.saveAllAndFlush(definitions.stream().map(Pair::getFirst).toList());

        List<QueueData> dataToQueue = new ArrayList<>();

        try {
            for (long run = 1; run <= request.runCount(); run++) {
                ExperimentRun experimentRun = new ExperimentRun(experiment, run);
                experimentRunRepository.saveAndFlush(experimentRun);

                List<ExperimentPartExecution> runExecutions = new ArrayList<>();

                for (Pair<ExperimentPart, CreateExperimentRequestData> pair : definitions) {
                    ExperimentPart definition = pair.getFirst();
                    CreateExperimentRequestData requestData = pair.getSecond();

                    ExperimentPartExecution execution = new ExperimentPartExecution(definition);
                    experimentRun.addPart(execution);

                    for (String indicatorName : requestData.indicators()) {
                        ExperimentPartIndicator indicator = new ExperimentPartIndicator(indicatorName, 0.0);
                        execution.addIndicator(indicator);
                    }

                    runExecutions.add(execution);
                }

                experimentPartExecutionRepository.saveAllAndFlush(runExecutions);

                for (int i = 0; i < definitions.size(); i++) {
                    ExperimentPart definition = definitions.get(i).getFirst();
                    CreateExperimentRequestData requestData = definitions.get(i).getSecond();
                    ExperimentPartExecution execution = runExecutions.get(i);

                    QueueData queueData = createQueueData(requestData, definition);
                    queueData.setExperimentPartId(execution.getId());
                    dataToQueue.add(queueData);
                }

                logger.info("Successfully created and queued new experiment run number {} for experiment {}",
                        experimentRun.getRunNo(), experimentRun.getExperimentId());
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info("Transaction committed. Sending {} tasks to queue.", dataToQueue.size());
                    for (QueueData data : dataToQueue) {
                        executionService.enqueue(data);
                    }
                }
            });

            logger.info("Successfully created and queued new experiment ID {}", experiment.getId());
            return experiment;

        } catch (Exception e) {
            logger.error("Failed to create experiment", e);
            throw e;
        }
    }

    private Pair<ExperimentPart, CreateExperimentRequestData> createExperimentPartDefinition(
            CreateExperimentRequestData partRequest) {
        logger.debug("Preparing ExperimentPart Definition: Problem={}, Algorithm={}", partRequest.problem(),
                partRequest.algorithm());

        String problemName = partRequest.problem();
        String algorithmName = partRequest.algorithm();
        var algorithmParameters = algorithmRegistry.createTypedProperties(partRequest.algorithmParameters());

        Problem problem = problemRegistry.getProblem(problemName)
                .orElseThrow(() -> new ProblemNotFoundException(problemName));

        algorithmRegistry.getAlgorithm(algorithmName, algorithmParameters, problem)
                .orElseThrow(() -> new AlgorithmNotFoundException(algorithmName));

        ExperimentPart experimentPart = new ExperimentPart(
                problemName,
                algorithmName,
                partRequest.budget());
        for (String key : algorithmParameters.keySet()) {
            String value = algorithmParameters.getString(key);
            ExperimentPartAlgorithmParameter parameterEntity = new ExperimentPartAlgorithmParameter(key, value);
            experimentPart.addParameter(parameterEntity);
        }
        algorithmParameters.clearAccessedProperties();

        return Pair.of(experimentPart, partRequest);
    }

    private QueueData createQueueData(CreateExperimentRequestData partRequest, ExperimentPart definition) {
        String problemName = definition.getProblem();
        String algorithmName = definition.getAlgorithm();
        Problem problem = problemRegistry.getProblem(problemName).orElseThrow();
        NondominatedPopulation referenceSet = problemRegistry.getReferenceSet(problemName).orElseThrow();

        var algorithmParameters = algorithmRegistry.createTypedProperties(partRequest.algorithmParameters());

        Algorithm algorithm = algorithmRegistry.getAlgorithm(algorithmName, algorithmParameters, problem).orElseThrow();
        Indicators indicatorsObj = indicatorRegistry.getIndicators(partRequest.indicators(), problem, referenceSet);

        return new QueueData(algorithm, indicatorsObj, definition.getBudget(), referenceSet, problem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Experiment> getExperiments(String algorithm, String problem, String indicator,
            ExperimentStatus status, OffsetDateTime start, OffsetDateTime end) {
        return experimentRepository.findAllFiltered(algorithm, problem, indicator, status, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Experiment> getExperiment(long id, ExperimentRunStatus status) {
        logger.debug("Fetching details for experiment ID {}", id);
        return experimentRepository.findWithRunsById(id)
                .map(exp -> {
                    if (status == null)
                        return exp;
                    exp.getRuns().removeIf(run -> run.getStatus() != status);
                    return exp;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExperimentRun> getExperimentRuns(String algorithm, String problem, String indicator,
            ExperimentRunStatus status, OffsetDateTime start, OffsetDateTime end,
            Pageable pageable) {
        return experimentRunRepository.findAllFiltered(algorithm, problem, indicator, status, start, end, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentRun> getExperimentRun(
            long id, long runNo, String algorithm, String problem,
            String indicator, ExperimentPartStatus partStatus) {

        RunId runId = new RunId(id, runNo);
        if (!experimentRunRepository.existsById(runId)) {
            return Optional.empty();
        }

        List<ExperimentPartExecution> filteredParts = experimentPartExecutionRepository.findFilteredParts(
                runId, algorithm, problem, partStatus, indicator);

        return experimentRunRepository.findById(runId).map(run -> {
            ExperimentRun viewRun = new ExperimentRun();
            viewRun.setStatus(run.getStatus());
            viewRun.setStartedAt(run.getStartedAt());
            viewRun.setFinishedAt(run.getFinishedAt());
            filteredParts.forEach(viewRun::addPart);
            return viewRun;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentPartExecution> getExperimentPart(long id, long runNo, long partId) {
        logger.debug("Fetching details for experiment part execution ID {} (Experiment ID {} run {})", partId, id,
                runNo);
        return experimentPartExecutionRepository.findByExperimentIdAndId(new RunId(id, runNo), partId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentStatus> getExperimentStatus(long id) {
        return experimentRepository.findStatusById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentRunStatus> getExperimentRunStatus(long id, long runNo) {
        return experimentRunRepository.findStatusById(new RunId(id, runNo));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PartStatusInfo> getExperimentPartStatus(long id, long runNo, long partId) {
        return experimentPartExecutionRepository.findStatusInfoById(new RunId(id, runNo), partId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Experiment> getExperimentResult(long id) {
        logger.debug("Fetching results for experiment ID {}", id);
        return experimentRepository.findWithSolutionById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentRun> getExperimentRunResult(long id, long runNo) {
        logger.debug("Fetching results for experiment ID {} run {}", id, runNo);
        return experimentRunRepository.findWithFullSolutionById(new RunId(id, runNo));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentPartExecution> getExperimentPartResult(long id, long runNo, long partId) {
        logger.debug("Fetching results for experiment part ID {}", partId);
        return experimentPartExecutionRepository.findWithFullSolutionById(new RunId(id, runNo), partId);
    }

    @Override
    @Transactional
    public boolean deleteExperiment(long id) {
        logger.debug("Attempting to delete experiment ID {}", id);

        return experimentRepository.findById(id).map(experiment -> {

            for (ExperimentRun run : experiment.getRuns()) {
                var executions = experimentPartExecutionRepository.findAllByExperimentRunId(run.getId());
                if (!executions.isEmpty()) {
                    experimentPartExecutionRepository.deleteAll(executions);
                }
            }

            experiment.getRuns().clear();
            experimentRepository.save(experiment);

            experiment.getParts().clear();
            experimentRepository.save(experiment);

            experimentRepository.delete(experiment);
            logger.info("Experiment ID {} deleted successfully", id);
            return true;

        }).orElseGet(() -> {
            logger.info("Failed to delete experiment ID {}: Not found", id);
            return false;
        });
    }

    @Override
    @Transactional
    public boolean deleteExperimentRun(long id, long runNo) {
        logger.debug("Attempting to delete experiment ID {} run {}", id, runNo);

        return experimentRepository.findWithRunsById(id).map(experiment -> {
            Optional<ExperimentRun> runToDelete = experiment.getRuns().stream()
                    .filter(r -> r.getRunNo().equals(runNo))
                    .findFirst();

            if (runToDelete.isPresent()) {
                ExperimentRun run = runToDelete.get();

                var executions = experimentPartExecutionRepository.findAllByExperimentRunId(run.getId());
                if (!executions.isEmpty()) {
                    experimentPartExecutionRepository.deleteAll(executions);
                }

                experiment.removeRun(run);

                if (experiment.getRuns().isEmpty()) {
                    logger.info("Deleting experiment ID {} because last run was removed", id);

                    experiment.getRuns().clear();
                    experimentRepository.save(experiment);

                    experiment.getParts().clear();
                    experimentRepository.save(experiment);

                    experimentRepository.delete(experiment);
                } else {
                    experimentRepository.save(experiment);
                    logger.info("Experiment ID {} run {} deleted successfully", id, runNo);
                }
                return true;
            }

            return false;
        }).orElseGet(() -> {
            logger.info("Failed to delete experiment ID {} run {}: Experiment not found", id, runNo);
            return false;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public GetExperimentAggregateResponse getExperimentAggregate(long experimentId) {
        logger.debug("Calculating aggregate statistics for experiment ID {}", experimentId);
        if (!experimentRepository.existsById(experimentId)) {
            throw new NotFoundException("Experiment not found");
        }
        List<ExperimentPart> definitions = experimentPartRepository.findAllByExperimentId(experimentId);

        List<Object[]> rawRows = experimentAggregateRepository.findIndicatorAggregatesByExperimentId(experimentId);
        List<IndicatorAggregateRow> rows = rawRows.stream()
                .map(IndicatorAggregateRow::fromNativeRow)
                .toList();

        Map<Long, Map<String, GetExperimentAggregateDataIndicator>> indicatorsByDefinition = new HashMap<>();
        for (IndicatorAggregateRow row : rows) {
            indicatorsByDefinition
                    .computeIfAbsent(row.definitionId(), _ -> new HashMap<>())
                    .put(row.indicator(),
                            new GetExperimentAggregateDataIndicator(
                                    row.minValue(),
                                    row.maxValue(),
                                    row.meanValue(),
                                    row.medianValue(),
                                    row.iqrValue(),
                                    row.stddevValue()));
        }
        List<GetExperimentAggregateData> aggregateDataList = new ArrayList<>();
        for (ExperimentPart definition : definitions) {
            Map<String, String> params = definition.getParameters().stream()
                    .collect(Collectors.toMap(
                            ExperimentPartAlgorithmParameter::getKey,
                            ExperimentPartAlgorithmParameter::getValue));

            aggregateDataList.add(new GetExperimentAggregateData(
                    definition.getAlgorithm(),
                    params,
                    definition.getProblem(),
                    indicatorsByDefinition.getOrDefault(definition.getId(), Collections.emptyMap()),
                    definition.getBudget()));
        }

        return new GetExperimentAggregateResponse(aggregateDataList);
    }


    @Override
    @Transactional(readOnly = true)
    public GetExperimentAggregateResponse getExperimentGroupAggregate(long groupId) {
        ExperimentGroup group = getGroupOrThrow(groupId);
        Map<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> indicatorsStructure =
                fetchAndStructureIndicators(groupId);
        List<GetExperimentAggregateData> responseList =
                buildAggregateDataList(indicatorsStructure, group);

        return new GetExperimentAggregateResponse(responseList);
    }

    private ExperimentGroup getGroupOrThrow(long groupId) {
        return experimentGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Experiment group not found"));
    }

    private Map<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> fetchAndStructureIndicators(long groupId) {
        List<Object[]> rawRows = experimentAggregateRepository.findIndicatorAggregatesByExperimentGroupId(groupId);
        Map<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> structure = new HashMap<>();
        for (Object[] rawRow : rawRows) {
            GroupIndicatorAggregateRow row = GroupIndicatorAggregateRow.fromNativeRow(rawRow);
            Map<String, Map<String, GetExperimentAggregateDataIndicator>> problemsMap = structure.computeIfAbsent(row.algorithm(), k -> new HashMap<>());

            Map<String, GetExperimentAggregateDataIndicator> indicatorsMap = problemsMap.computeIfAbsent(row.problem(), k -> new HashMap<>());

            indicatorsMap.put(row.indicator(), new GetExperimentAggregateDataIndicator(
                    row.minValue(), row.maxValue(), row.meanValue(),
                    row.medianValue(), row.iqrValue(), row.stddevValue()
            ));
        }

        return structure;
    }

    private List<GetExperimentAggregateData> buildAggregateDataList(
            Map<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> structure,
            ExperimentGroup group) {

        List<GetExperimentAggregateData> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> algoEntry : structure.entrySet()) {
            String algorithm = algoEntry.getKey();
            Map<String, Map<String, GetExperimentAggregateDataIndicator>> problemsMap = algoEntry.getValue();

            for (Map.Entry<String, Map<String, GetExperimentAggregateDataIndicator>> problemEntry : problemsMap.entrySet()) {
                String problem = problemEntry.getKey();
                Map<String, GetExperimentAggregateDataIndicator> indicators = problemEntry.getValue();
                ExperimentPartMetadata metadata = findMetadataFor(group, algorithm, problem);
                result.add(new GetExperimentAggregateData(
                        algorithm,
                        metadata.params,
                        problem,
                        indicators,
                        metadata.budget
                ));
            }
        }

        return result;
    }

    private static class ExperimentPartMetadata {
        int budget = 0;
        Map<String, String> params = Collections.emptyMap();
    }

    private ExperimentPartMetadata findMetadataFor(ExperimentGroup group, String algorithm, String problem) {
        ExperimentPartMetadata metadata = new ExperimentPartMetadata();
        if (group.getRuns() == null || group.getRuns().isEmpty()) {
            return metadata;
        }
        ExperimentRun representativeRun = group.getRuns().iterator().next();

        for (ExperimentPartExecution epe : representativeRun.getParts()) {
            ExperimentPart part = epe.getExperimentPart();
            if (part.getAlgorithm().equals(algorithm) && part.getProblem().equals(problem)) {
                metadata.budget = part.getBudget();
                metadata.params = new HashMap<>();
                for (ExperimentPartAlgorithmParameter param : part.getParameters()) {
                    metadata.params.put(param.getKey(), param.getValue());
                }
                break;
            }
        }

        return metadata;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getExperimentPartCsv(long id, long runNo, long partId) {
        return getExperimentPartResult(id, runNo, partId).map(part -> {
            Set<ExperimentPartSolution> solutions = part.getSolutions();
            if (solutions.isEmpty()) {
                return "";
            }

            CsvStructure structure = extractCsvStructure(solutions);
            StringBuilder csv = new StringBuilder();
            csv.append(createHeader(structure)).append("\n");
            csv.append(createBody(solutions, structure));

            return csv.toString();
        });
    }

    private CsvStructure extractCsvStructure(Set<ExperimentPartSolution> solutions) {
        TreeSet<String> varKeys = new TreeSet<>();
        TreeSet<String> objKeys = new TreeSet<>();
        TreeSet<String> constKeys = new TreeSet<>();

        for (ExperimentPartSolution solution : solutions) {
            varKeys.addAll(solution.getVariables().keySet());
            objKeys.addAll(solution.getObjectives().keySet());
            constKeys.addAll(solution.getConstraints().keySet());
        }
        return new CsvStructure(varKeys, objKeys, constKeys);
    }

    private String createHeader(CsvStructure structure) {
        List<String> header = new ArrayList<>();
        structure.varKeys().forEach(k -> header.add("Var:" + k));
        structure.objKeys().forEach(k -> header.add("Obj:" + k));
        structure.constKeys().forEach(k -> header.add("Const:" + k));
        return String.join(",", header);
    }

    private String createBody(Set<ExperimentPartSolution> solutions, CsvStructure structure) {
        StringBuilder body = new StringBuilder();
        for (ExperimentPartSolution solution : solutions) {
            String row = createRow(solution, structure);
            body.append(row).append("\n");
        }
        return body.toString();
    }

    private String createRow(ExperimentPartSolution solution, CsvStructure structure) {
        List<String> row = new ArrayList<>();

        for (String k : structure.varKeys()) {
            row.add(solution.getVariables().getOrDefault(k, ""));
        }
        for (String k : structure.objKeys()) {
            row.add(String.valueOf(solution.getObjectives().getOrDefault(k, 0.0)));
        }
        for (String k : structure.constKeys()) {
            row.add(String.valueOf(solution.getConstraints().getOrDefault(k, 0.0)));
        }

        return String.join(",", row);
    }

    private record CsvStructure(Set<String> varKeys, Set<String> objKeys, Set<String> constKeys) {}

    @Override
    @Transactional
    public ExperimentGroup createExperimentGroup(String name) {
        ExperimentGroup group = new ExperimentGroup(name);
        return experimentGroupRepository.save(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentGroup> getExperimentGroups() {
        return experimentGroupRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentGroup> getExperimentGroup(long groupId) {
        return experimentGroupRepository.findById(groupId);
    }

    @Override
    @Transactional
    public ExperimentGroup addRunToExperimentGroup(Long groupId, Long id, Long runNo) {
        ExperimentGroup group = experimentGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("ExperimentGroup not found"));

        ExperimentRun run = experimentRunRepository.findById(new RunId(id, runNo))
                .orElseThrow(() -> new NotFoundException("ExperimentRun not found"));

        group.addRun(run);
        return experimentGroupRepository.save(group);
    }

    @Override
    @Transactional
    public boolean deleteExperimentGroup(long groupId) {
        if (experimentGroupRepository.existsById(groupId)) {
            experimentGroupRepository.deleteById(groupId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public ExperimentGroup deleteRunFromExperimentGroup(Long groupId, Long id, Long runNo) {
        ExperimentGroup group = experimentGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("ExperimentGroup not found"));

        ExperimentRun run = experimentRunRepository.findById(new RunId(id, runNo))
                .orElseThrow(() -> new NotFoundException("ExperimentRun not found"));

        if (!group.removeRun(run)) {
            throw new NotFoundException("Run " + runNo + " of experiment " + id + " is not part of group " + groupId);
        }
        return experimentGroupRepository.save(group);
    }

}