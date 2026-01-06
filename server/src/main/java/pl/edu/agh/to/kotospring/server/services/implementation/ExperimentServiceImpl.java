package pl.edu.agh.to.kotospring.server.services.implementation;

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
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRunRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.*;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequestData;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExperimentServiceImpl implements ExperimentService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentServiceImpl.class);

    private final ProblemRegistryService problemRegistry;
    private final AlgorithmRegistryService algorithmRegistry;
    private final IndicatorRegistryService indicatorRegistry;
    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentExecutionService executionService;
    private final ExperimentRepository experimentRepository;

    public ExperimentServiceImpl(ProblemRegistryService problemRegistry,
                                 AlgorithmRegistryService algorithmRegistry,
                                 IndicatorRegistryService indicatorRegistry,
                                 ExperimentRunRepository experimentRunRepository,
                                 ExperimentPartRepository experimentPartRepository,
                                 ExperimentExecutionService executionService, ExperimentRepository experimentRepository) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
        this.experimentRunRepository = experimentRunRepository;
        this.experimentPartRepository = experimentPartRepository;
        this.executionService = executionService;
        this.experimentRepository = experimentRepository;
    }

    @Override
    @Transactional
    public Experiment createExperiment(CreateExperimentRequest request) {
        logger.info("Received request to create new full experiment with {} runs, {} parts each", request.runCount(), request.parts().size());
        Experiment experiment = new Experiment(OffsetDateTime.now(), request.runCount());
        experimentRepository.saveAndFlush(experiment);
        List<QueueData> dataToQueue = new ArrayList<>();

        try {
            for (long run = 1; run <= request.runCount(); run++) {

                List<Pair<ExperimentPart, QueueData>> experimentParts = request.parts().stream()
                        .map(this::createExperimentPart)
                        .toList();

                ExperimentRun experimentRun = new ExperimentRun(experiment, run);
                experimentRunRepository.saveAndFlush(experimentRun);

                for (Pair<ExperimentPart, QueueData> pair : experimentParts) {
                    ExperimentPart newPart = pair.getFirst();
                    experimentRun.addPart(newPart);
                }

                experimentPartRepository.saveAllAndFlush(experimentParts.stream().map(Pair::getFirst).toList());

                for (Pair<ExperimentPart, QueueData> pair : experimentParts) {
                    pair.getSecond().setExperimentPartId(pair.getFirst().getId());
                    dataToQueue.add(pair.getSecond());
                }

                logger.info("Successfully created and queued new experiment run number {} for experiment {}", experimentRun.getRunNo(), experimentRun.getExperimentId());
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

    private Pair<ExperimentPart, QueueData> createExperimentPart(CreateExperimentRequestData partRequest) {
        logger.debug("Preparing ExperimentPart: Problem={}, Algorithm={}", partRequest.problem(), partRequest.algorithm());

        String problemName = partRequest.problem();
        String algorithmName = partRequest.algorithm();
        var algorithmParameters = algorithmRegistry.createTypedProperties(partRequest.algorithmParameters());
        var indicators = partRequest.indicators();
        int budget = partRequest.budget();

        Problem problem = problemRegistry.getProblem(problemName)
                .orElseThrow(() -> {
                    logger.info("Problem not found: {}", problemName);
                    return new ProblemNotFoundException(problemName);
                });
        NondominatedPopulation referenceSet = problemRegistry.getReferenceSet(problemName)
                .orElseThrow(() -> {
                    logger.info("Reference set for problem not found: {}", problemName);
                    return new ProblemNotFoundException(problemName);
                });
        Algorithm algorithm = algorithmRegistry.getAlgorithm(algorithmName, algorithmParameters, problem)
                .orElseThrow(() -> {
                    logger.info("Algorithm not found: {}", algorithmName);
                    return new AlgorithmNotFoundException(algorithmName);
                });
        Indicators indicatorsObj = indicatorRegistry.getIndicators(indicators, problem, referenceSet);

        ExperimentPart experimentPart = new ExperimentPart(
                problemName,
                algorithmName,
                budget
        );
        for (String key : algorithmParameters.keySet()) {
            String value = algorithmParameters.getString(key);
            ExperimentPartAlgorithmParameter parameterEntity = new ExperimentPartAlgorithmParameter(key, value);
            experimentPart.addParameter(parameterEntity);
        }
        algorithmParameters.clearAccessedProperties();
        for (String indicatorName : partRequest.indicators()) {
            ExperimentPartIndicator indicator = new ExperimentPartIndicator(indicatorName, 0.0);
            experimentPart.addIndicator(indicator);
        }
        QueueData queueData = new QueueData(algorithm, indicatorsObj, budget);
        return Pair.of(experimentPart, queueData);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Experiment> getExperiments() {
        logger.debug("Fetching all experiments");
        return experimentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Experiment> getExperiment(long id) {
        logger.debug("Fetching details for experiment ID {}", id);
        return experimentRepository.findWithRunsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentRun> getExperimentRun(long id, long runNo) {
        logger.debug("Fetching details for experiment ID {} run {}", id, runNo);
        return experimentRunRepository.findWithPartsById(new RunId(id, runNo));
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentPart> getExperimentPart(long id, long runNo, long partId) {
        logger.debug("Fetching details for experiment part ID {} (Experiment ID {} run {})", partId, id, runNo);
        return experimentPartRepository.findByExperimentIdAndId(new RunId(id, runNo), partId);
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
    public Optional<ExperimentPart> getExperimentPartResult(long id, long runNo, long partId) {
        logger.debug("Fetching results for experiment part ID {}", id);
        return experimentPartRepository.findWithFullSolutionById(id, partId);
    }

    @Override
    @Transactional
    public boolean deleteExperiment(long id) {
        logger.debug("Attempting to delete experiment ID {}", id);

        if (experimentRepository.existsById(id)) {
            experimentRepository.deleteById(id);
            logger.info("Experiment ID {} deleted successfully", id);
            return true;
        }

        logger.info("Failed to delete experiment ID {}: Not found", id);
        return false;
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
}