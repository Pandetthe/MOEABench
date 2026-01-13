package pl.edu.agh.to.kotospring.server.services.implementation;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.exceptions.AlgorithmNotFoundException;
import pl.edu.agh.to.kotospring.server.exceptions.ProblemNotFoundException;
import pl.edu.agh.to.kotospring.server.models.PartStatusInfo;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartExecutionRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRunRepository;
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

    public ExperimentServiceImpl(ProblemRegistryService problemRegistry,
            AlgorithmRegistryService algorithmRegistry,
            IndicatorRegistryService indicatorRegistry,
            ExperimentRunRepository experimentRunRepository,
            ExperimentPartRepository experimentPartRepository,
            ExperimentPartExecutionRepository experimentPartExecutionRepository,
            ExperimentExecutionService executionService, ExperimentRepository experimentRepository) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
        this.experimentRunRepository = experimentRunRepository;
        this.experimentPartRepository = experimentPartRepository;
        this.experimentPartExecutionRepository = experimentPartExecutionRepository;
        this.executionService = executionService;
        this.experimentRepository = experimentRepository;
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

        return new QueueData(algorithm, indicatorsObj, definition.getBudget());
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
            experiment.getRuns().clear();
            experimentRepository.saveAndFlush(experiment);

            experiment.getParts().clear();
            experimentRepository.saveAndFlush(experiment);

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
                experiment.removeRun(runToDelete.get());
                if (experiment.getRuns().isEmpty()) {
                    logger.info("Deleting experiment ID {} because last run was removed", id);
                    experiment.getRuns().clear();
                    experimentRepository.saveAndFlush(experiment);

                    experiment.getParts().clear();
                    experimentRepository.saveAndFlush(experiment);

                    experimentRepository.delete(experiment);
                } else {
                    experimentRepository.saveAndFlush(experiment);
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
    public GetExperimentAggregateResponse getExperimentAggregate(
            long experimentId) {
        logger.debug("Calculating aggregate statistics for experiment ID {}", experimentId);
        List<ExperimentPart> definitions = experimentPartRepository.findAllByExperimentId(experimentId);

        List<GetExperimentAggregateData> aggregateDataList = new ArrayList<>();

        for (ExperimentPart definition : definitions) {

            Experiment experiment = experimentRepository.findWithRunsById(experimentId).orElseThrow(
                    () -> new NotFoundException("Experiment not found"));

            List<ExperimentPartExecution> executionsForDef = new ArrayList<>();
            for (ExperimentRun run : experiment.getRuns()) {
                run.getParts().stream()
                        .filter(p -> p.getExperimentPart().equals(definition))
                        .forEach(executionsForDef::add);
            }

            Set<String> indicatorNames = executionsForDef.stream()
                    .flatMap(e -> e.getIndicators().stream())
                    .map(ExperimentPartIndicator::getName)
                    .collect(Collectors.toSet());

            Map<String, GetExperimentAggregateDataIndicator> indicatorsMap = new HashMap<>();

            for (String indicator : indicatorNames) {
                DescriptiveStatistics stats = new DescriptiveStatistics();
                executionsForDef.stream()
                        .flatMap(e -> e.getIndicators().stream())
                        .filter(i -> i.getName().equals(indicator))
                        .forEach(i -> stats.addValue(i.getValue()));

                if (stats.getN() > 0) {
                    indicatorsMap.put(indicator,
                            new GetExperimentAggregateDataIndicator(
                                    stats.getMin(),
                                    stats.getMax(),
                                    stats.getMean(),
                                    stats.getPercentile(50), // Median
                                    stats.getPercentile(75) - stats.getPercentile(25), // IQR
                                    stats.getStandardDeviation()));
                }
            }

            aggregateDataList.add(new GetExperimentAggregateData(
                    definition.getAlgorithm(),
                    definition.getParameters().stream().collect(Collectors.toMap(
                            ExperimentPartAlgorithmParameter::getKey,
                            ExperimentPartAlgorithmParameter::getValue)),
                    definition.getProblem(),
                    indicatorsMap,
                    definition.getBudget()));
        }

        return new GetExperimentAggregateResponse(
                aggregateDataList);
    }
}