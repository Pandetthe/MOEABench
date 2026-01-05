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
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartAlgorithmParameter;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartIndicator;
import pl.edu.agh.to.kotospring.server.exceptions.AlgorithmNotFoundException;
import pl.edu.agh.to.kotospring.server.exceptions.ProblemNotFoundException;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.*;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequestData;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExperimentServiceImpl implements ExperimentService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentServiceImpl.class);

    private final ProblemRegistryService problemRegistry;
    private final AlgorithmRegistryService algorithmRegistry;
    private final IndicatorRegistryService indicatorRegistry;
    private final ExperimentRepository experimentRepository;
    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentExecutionService executionService;

    public ExperimentServiceImpl(ProblemRegistryService problemRegistry,
                                 AlgorithmRegistryService algorithmRegistry,
                                 IndicatorRegistryService indicatorRegistry,
                                 ExperimentRepository experimentRepository,
                                 ExperimentPartRepository experimentPartRepository,
                                 ExperimentExecutionService executionService) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
        this.experimentRepository = experimentRepository;
        this.experimentPartRepository = experimentPartRepository;
        this.executionService = executionService;
    }

    @Override
    @Transactional
    public Experiment createExperiment(CreateExperimentRequest request) {
        logger.info("Received request to create new experiment with {} parts", request.size());

        try {
            List<Pair<ExperimentPart, QueueData>> experimentParts = request.stream()
                    .map(this::createExperimentPart)
                    .toList();

            List<ExperimentPart> experimentPartList = experimentParts.stream()
                    .map(Pair::getFirst)
                    .collect(Collectors.toList());

            List<QueueData> queueDataList = experimentParts.stream()
                    .map(Pair::getSecond)
                    .toList();

            Experiment experiment = new Experiment(OffsetDateTime.now(), experimentPartList);
            experimentRepository.saveAndFlush(experiment);
            logger.debug("Experiment entity saved with ID {}", experiment.getId());

            for (int i = 0; i < experimentPartList.size(); i++) {
                ExperimentPart savedPart = experimentPartList.get(i);
                QueueData queueData = queueDataList.get(i);
                queueData.setExperimentPartId(savedPart.getId());
                executionService.enqueue(queueData);
            }

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

        List<ExperimentPartAlgorithmParameter> algorithmParameterEntities = new ArrayList<>(algorithmParameters.size());
        for (String key : algorithmParameters.keySet()) {
            String value = algorithmParameters.getString(key);
            ExperimentPartAlgorithmParameter parameterEntity = new ExperimentPartAlgorithmParameter(key, value);
            algorithmParameterEntities.add(parameterEntity);
        }
        algorithmParameters.clearAccessedProperties();

        ExperimentPart experimentPart = new ExperimentPart(
                problemName,
                algorithmName,
                algorithmParameterEntities,
                budget
        );
        for (String indicatorName : partRequest.indicators()) {
            ExperimentPartIndicator indicator = new ExperimentPartIndicator(indicatorName, 0.0);
            indicator.setExperimentPart(experimentPart);
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
        return experimentRepository.findWithPartsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Experiment> getExperimentStatus(long id) {
        logger.debug("Fetching status for experiment ID {}", id);
        return experimentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentPart> getExperimentPart(long id, long partId) {
        logger.debug("Fetching details for experiment part ID {} (Experiment ID {})", partId, id);
        return experimentPartRepository.findByExperimentIdAndId(id, partId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Experiment> getExperimentResult(long id) {
        logger.debug("Fetching results for experiment ID {}", id);
        return experimentRepository.findWithFullSolutionById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentPart> getExperimentPartResult(long id, long partId) {
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
}