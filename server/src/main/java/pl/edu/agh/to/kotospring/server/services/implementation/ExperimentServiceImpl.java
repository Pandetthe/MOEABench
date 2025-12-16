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
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.services.interfaces.AlgorithmRegistryService;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentService;
import pl.edu.agh.to.kotospring.server.services.interfaces.IndicatorRegistryService;
import pl.edu.agh.to.kotospring.server.services.interfaces.ProblemRegistryService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExperimentServiceImpl implements ExperimentService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentServiceImpl.class);

    private final ProblemRegistryService problemRegistry;
    private final AlgorithmRegistryService algorithmRegistry;
    private final IndicatorRegistryService indicatorRegistry;
    private final ExperimentRepository experimentRepository;
    private final ExperimentExecutionService executionService;

    public ExperimentServiceImpl(ProblemRegistryService problemRegistry,
                                 AlgorithmRegistryService algorithmRegistry,
                                 IndicatorRegistryService indicatorRegistry,
                                 ExperimentRepository experimentRepository, ExperimentExecutionService executionService) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
        this.experimentRepository = experimentRepository;
        this.executionService = executionService;
    }

    @Override
    @Transactional
    public CreateExperimentResponse createExperiment(CreateExperimentRequest request) {
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
        for (ExperimentPart part : experimentPartList) {
            part.setExperiment(experiment);
        }
        experimentRepository.save(experiment);

        // TODO: Queue experiment parts

        for (int i = 0; i < experimentPartList.size(); i++) {
            logger.info("in loop");
            ExperimentPart savedPart = experimentPartList.get(i);
            QueueData originalQueueData = queueDataList.get(i);

            QueueData readyToRunData = new QueueData(
                    savedPart.getId(),
                    originalQueueData.getAlgorithm(),
                    originalQueueData.getIndicators(),
                    originalQueueData.getBudget()
            );
            logger.info("id = {}", readyToRunData.getExperimentPartId());
            logger.info("algo = {}", readyToRunData.getAlgorithm());

            executionService.runExperimentPart(readyToRunData);
            if (i == 0) {
                experiment.setStartedAt(OffsetDateTime.now());
                experiment.setStatus(ExperimentStatus.IN_PROGRESS);
                experimentRepository.save(experiment);
            }
        }
        experiment.setFinishedAt(OffsetDateTime.now());
        experiment.setStatus(ExperimentStatus.SUCCESS);
        experimentRepository.save(experiment);


        return new CreateExperimentResponse(experiment.getId());
    }

    private Pair<ExperimentPart, QueueData> createExperimentPart(CreateExperimentRequestData partRequest) {
        String problemName = partRequest.problem();
        String algorithmName = partRequest.algorithm();
        var algorithmParameters = algorithmRegistry.createTypedProperties(partRequest.algorithmParameters());
        var indicators = partRequest.indicators();
        int budget = partRequest.budget();

        Problem problem = problemRegistry.getProblem(problemName);
        NondominatedPopulation referenceSet = problemRegistry.getReferenceSet(problemName);
        Algorithm algorithm = algorithmRegistry.getAlgorithm(algorithmName, algorithmParameters, problem);
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
            experimentPart.getIndicators().add(indicator);
        }
        QueueData queueData = new QueueData(experimentPart.getId(), algorithm, indicatorsObj, budget);
        return Pair.of(experimentPart, queueData);
    }

    @Override
    public GetExperimentsResponse getExperiments() {
        return experimentRepository.findAll()
                .stream()
                .map(experiment -> new GetExperimentsResponseData(
                        experiment.getId(),
                        experiment.getStatus(),
                        experiment.getQueuedAt(),
                        experiment.getStartedAt(),
                        experiment.getFinishedAt()
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        GetExperimentsResponse::new
                ));
    }

    @Override
    public Optional<GetExperimentResponse> getExperiment(long id) {
        return experimentRepository.findById(id)
                .map(experiment -> new GetExperimentResponse(
                        experiment.getStatus(),
                        experiment.getQueuedAt(),
                        experiment.getStartedAt(),
                        experiment.getFinishedAt()
                ));
    }

    @Override
    public Optional<GetExperimentStatusResponse> getExperimentStatus(long id) {
        return experimentRepository.findById(id)
                .map(experiment -> new GetExperimentStatusResponse(
                        experiment.getStatus()
                ));
    }

    @Override
    public Optional<GetExperimentPartStatusResponse> getExperimentStatus(long id, long partId) {
        return Optional.empty();
    }

    @Override
    public Optional<GetExperimentResultResponse> getExperimentResult(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<GetExperimentPartResultResponse> getExperimentResult(long id, long partId) {
        return Optional.empty();
    }

    @Override
    public boolean deleteExperiment(long id) {
        if(experimentRepository.existsById(id)){
            experimentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
