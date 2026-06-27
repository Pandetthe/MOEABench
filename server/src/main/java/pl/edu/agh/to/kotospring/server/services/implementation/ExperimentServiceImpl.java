package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.exceptions.AlgorithmNotFoundException;
import pl.edu.agh.to.kotospring.server.exceptions.NotAllPartsFinishedException;
import pl.edu.agh.to.kotospring.server.exceptions.ProblemNotFoundException;
import pl.edu.agh.to.kotospring.server.models.ExperimentRunView;
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

        List<ExperimentPartWithRequest> definitions = request.parts().stream()
                .map(this::createExperimentPartDefinition)
                .toList();

        definitions.forEach(pair -> experiment.addPart(pair.part()));
        experimentPartRepository.saveAll(definitions.stream().map(ExperimentPartWithRequest::part).toList());

        List<QueueData> dataToQueue = new ArrayList<>();

        try {
            for (long run = 1; run <= request.runCount(); run++) {
                ExperimentRun experimentRun = new ExperimentRun(experiment, run);
                experimentRunRepository.save(experimentRun);

                List<ExperimentPartExecution> runExecutions = new ArrayList<>();

                for (ExperimentPartWithRequest pair : definitions) {
                    ExperimentPart definition = pair.part();
                    CreateExperimentRequestData requestData = pair.requestData();

                    ExperimentPartExecution execution = new ExperimentPartExecution(definition);
                    experimentRun.addPart(execution);

                    for (String indicatorName : requestData.indicators()) {
                        ExperimentPartIndicator indicator = new ExperimentPartIndicator(indicatorName, null);
                        execution.addIndicator(indicator);
                    }

                    runExecutions.add(execution);
                }

                experimentPartExecutionRepository.saveAll(runExecutions);

                for (int i = 0; i < definitions.size(); i++) {
                    ExperimentPartWithRequest pair = definitions.get(i);
                    ExperimentPartExecution execution = runExecutions.get(i);
                    dataToQueue.add(createQueueData(pair.requestData(), pair.part(), execution.getId()));
                }

                logger.info("Successfully created and queued new experiment run number {} for experiment {}",
                        experimentRun.getRunNo(), experiment.getId());
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info("Transaction committed. Sending {} tasks to queue.", dataToQueue.size());
                    dataToQueue.forEach(executionService::enqueue);
                }
            });

            logger.info("Successfully created and queued new experiment ID {}", experiment.getId());
            return experiment;

        } catch (Exception e) {
            logger.error("Failed to create experiment", e);
            throw e;
        }
    }

    private ExperimentPartWithRequest createExperimentPartDefinition(CreateExperimentRequestData partRequest) {
        logger.debug("Preparing ExperimentPart Definition: Problem={}, Algorithm={}", partRequest.problem(),
                partRequest.algorithm());

        String problemName = partRequest.problem();
        String algorithmName = partRequest.algorithm();
        var algorithmParameters = algorithmRegistry.createTypedProperties(partRequest.algorithmParameters());

        Problem problem = problemRegistry.getProblem(problemName)
                .orElseThrow(() -> new ProblemNotFoundException(problemName));

        algorithmRegistry.getAlgorithm(algorithmName, algorithmParameters, problem)
                .orElseThrow(() -> new AlgorithmNotFoundException(algorithmName));

        ExperimentPart experimentPart = new ExperimentPart(problemName, algorithmName, partRequest.budget());
        for (String key : algorithmParameters.keySet()) {
            String value = algorithmParameters.getString(key);
            experimentPart.addParameter(new ExperimentPartAlgorithmParameter(key, value));
        }
        algorithmParameters.clearAccessedProperties();

        return new ExperimentPartWithRequest(experimentPart, partRequest);
    }

    private QueueData createQueueData(CreateExperimentRequestData partRequest, ExperimentPart definition,
            Long experimentPartId) {
        String problemName = definition.getProblem();
        String algorithmName = definition.getAlgorithm();
        Problem problem = problemRegistry.getProblem(problemName).orElseThrow();
        NondominatedPopulation referenceSet = problemRegistry.getReferenceSet(problemName).orElseThrow();
        var algorithmParameters = algorithmRegistry.createTypedProperties(partRequest.algorithmParameters());
        Algorithm algorithm = algorithmRegistry.getAlgorithm(algorithmName, algorithmParameters, problem).orElseThrow();
        Indicators indicatorsObj = indicatorRegistry.getIndicators(partRequest.indicators(), problem, referenceSet);
        return new QueueData(experimentPartId, algorithm, indicatorsObj, definition.getBudget(), referenceSet, problem);
    }

    private record ExperimentPartWithRequest(ExperimentPart part, CreateExperimentRequestData requestData) {}

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
        if (status == null) {
            return experimentRepository.findWithRunsById(id);
        }
        return experimentRepository.findWithRunsByIdAndStatus(id, status);
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
    public Optional<ExperimentRunView> getExperimentRun(
            long id, long runNo, String algorithm, String problem,
            String indicator, ExperimentPartStatus partStatus) {

        RunId runId = new RunId(id, runNo);
        return experimentRunRepository.findById(runId).map(run -> {
            List<ExperimentPartExecution> filteredParts = experimentPartExecutionRepository.findFilteredParts(
                    runId, algorithm, problem, partStatus, indicator);
            return new ExperimentRunView(run.getStatus(), run.getStartedAt(), run.getFinishedAt(), filteredParts);
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
        return experimentRepository.findWithSolutionById(id).map(exp -> {
            if (exp.getStatus() == ExperimentStatus.QUEUED || exp.getStatus() == ExperimentStatus.IN_PROGRESS) {
                throw new NotAllPartsFinishedException();
            }
            return exp;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentRun> getExperimentRunResult(long id, long runNo) {
        logger.debug("Fetching results for experiment ID {} run {}", id, runNo);
        return experimentRunRepository.findWithFullSolutionById(new RunId(id, runNo)).map(run -> {
            if (run.getStatus() == ExperimentRunStatus.QUEUED || run.getStatus() == ExperimentRunStatus.IN_PROGRESS) {
                throw new NotAllPartsFinishedException();
            }
            return run;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentPartExecution> getExperimentPartResult(long id, long runNo, long partId) {
        logger.debug("Fetching results for experiment part ID {}", partId);
        return experimentPartExecutionRepository.findWithFullSolutionById(new RunId(id, runNo), partId).map(part -> {
            if (part.getStatus() == ExperimentPartStatus.QUEUED || part.getStatus() == ExperimentPartStatus.RUNNING) {
                throw new NotAllPartsFinishedException();
            }
            return part;
        });
    }

    @Override
    @Transactional
    public boolean deleteExperiment(long id) {
        logger.debug("Attempting to delete experiment ID {}", id);
        if (!experimentRepository.existsById(id)) {
            logger.info("Failed to delete experiment ID {}: Not found", id);
            return false;
        }
        experimentRepository.deleteById(id);
        logger.info("Experiment ID {} deleted successfully", id);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteExperimentRun(long id, long runNo) {
        logger.debug("Attempting to delete experiment ID {} run {}", id, runNo);
        RunId runId = new RunId(id, runNo);
        if (!experimentRunRepository.existsById(runId)) {
            logger.info("Failed to delete experiment ID {} run {}: Not found", id, runNo);
            return false;
        }
        long runCount = experimentRunRepository.countByIdExperimentId(id);
        if (runCount == 1) {
            logger.info("Deleting experiment ID {} because last run was removed", id);
            experimentRepository.deleteById(id);
        } else {
            experimentRunRepository.deleteById(runId);
            logger.info("Experiment ID {} run {} deleted successfully", id, runNo);
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public GetExperimentAggregateResponse getExperimentAggregate(long experimentId) {
        logger.debug("Calculating aggregate statistics for experiment ID {}", experimentId);
        List<ExperimentPart> definitions = experimentPartRepository.findAllByExperimentId(experimentId);
        if (definitions.isEmpty() && !experimentRepository.existsById(experimentId)) {
            throw new NotFoundException("Experiment not found");
        }

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
                                    row.minValue(), row.maxValue(), row.meanValue(),
                                    row.medianValue(), row.iqrValue(), row.stddevValue()));
        }

        List<GetExperimentAggregateData> aggregateDataList = definitions.stream()
                .map(definition -> {
                    Map<String, String> params = definition.getParameters().stream()
                            .collect(Collectors.toMap(
                                    ExperimentPartAlgorithmParameter::getKey,
                                    ExperimentPartAlgorithmParameter::getValue));
                    return new GetExperimentAggregateData(
                            definition.getAlgorithm(),
                            params,
                            definition.getProblem(),
                            indicatorsByDefinition.getOrDefault(definition.getId(), Collections.emptyMap()),
                            definition.getBudget());
                })
                .toList();

        return new GetExperimentAggregateResponse(aggregateDataList);
    }

    @Override
    @Transactional(readOnly = true)
    public GetExperimentAggregateResponse getExperimentGroupAggregate(long groupId) {
        ExperimentGroup group = getGroupOrThrow(groupId);
        Map<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> indicatorsStructure =
                fetchAndStructureIndicators(groupId);
        List<GetExperimentAggregateData> responseList = buildAggregateDataList(indicatorsStructure, group);
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
            structure.computeIfAbsent(row.algorithm(), k -> new HashMap<>())
                    .computeIfAbsent(row.problem(), k -> new HashMap<>())
                    .put(row.indicator(), new GetExperimentAggregateDataIndicator(
                            row.minValue(), row.maxValue(), row.meanValue(),
                            row.medianValue(), row.iqrValue(), row.stddevValue()));
        }
        return structure;
    }

    private List<GetExperimentAggregateData> buildAggregateDataList(
            Map<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> structure,
            ExperimentGroup group) {

        List<GetExperimentAggregateData> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, GetExperimentAggregateDataIndicator>>> algoEntry : structure.entrySet()) {
            String algorithm = algoEntry.getKey();
            for (Map.Entry<String, Map<String, GetExperimentAggregateDataIndicator>> problemEntry : algoEntry.getValue().entrySet()) {
                String problem = problemEntry.getKey();
                ExperimentPartMetadata metadata = findMetadataFor(group, algorithm, problem);
                result.add(new GetExperimentAggregateData(
                        algorithm, metadata.params(), problem, problemEntry.getValue(), metadata.budget()));
            }
        }
        return result;
    }

    private record ExperimentPartMetadata(int budget, Map<String, String> params) {
        static ExperimentPartMetadata empty() {
            return new ExperimentPartMetadata(0, Collections.emptyMap());
        }
    }

    private ExperimentPartMetadata findMetadataFor(ExperimentGroup group, String algorithm, String problem) {
        if (group.getRuns().isEmpty()) {
            return ExperimentPartMetadata.empty();
        }
        ExperimentRun representativeRun = group.getRuns().iterator().next();
        for (ExperimentPartExecution epe : representativeRun.getParts()) {
            ExperimentPart part = epe.getExperimentPart();
            if (part.getAlgorithm().equals(algorithm) && part.getProblem().equals(problem)) {
                Map<String, String> params = new HashMap<>();
                for (ExperimentPartAlgorithmParameter param : part.getParameters()) {
                    params.put(param.getKey(), param.getValue());
                }
                return new ExperimentPartMetadata(part.getBudget(), params);
            }
        }
        return ExperimentPartMetadata.empty();
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
            return createHeader(structure) + "\n" + createBody(solutions, structure);
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
        structure.varKeys().forEach(k -> header.add("Var:" + escapeCsv(k)));
        structure.objKeys().forEach(k -> header.add("Obj:" + escapeCsv(k)));
        structure.constKeys().forEach(k -> header.add("Const:" + escapeCsv(k)));
        return String.join(",", header);
    }

    private String createBody(Set<ExperimentPartSolution> solutions, CsvStructure structure) {
        StringBuilder body = new StringBuilder();
        for (ExperimentPartSolution solution : solutions) {
            body.append(createRow(solution, structure)).append("\n");
        }
        return body.toString();
    }

    private String createRow(ExperimentPartSolution solution, CsvStructure structure) {
        List<String> row = new ArrayList<>();
        for (String k : structure.varKeys()) {
            row.add(escapeCsv(solution.getVariables().getOrDefault(k, "")));
        }
        for (String k : structure.objKeys()) {
            row.add(escapeCsv(String.valueOf(solution.getObjectives().getOrDefault(k, 0.0))));
        }
        for (String k : structure.constKeys()) {
            row.add(escapeCsv(String.valueOf(solution.getConstraints().getOrDefault(k, 0.0))));
        }
        return String.join(",", row);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private record CsvStructure(Set<String> varKeys, Set<String> objKeys, Set<String> constKeys) {}

    @Override
    @Transactional
    public ExperimentGroup createExperimentGroup(String name) {
        return experimentGroupRepository.save(new ExperimentGroup(name));
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
