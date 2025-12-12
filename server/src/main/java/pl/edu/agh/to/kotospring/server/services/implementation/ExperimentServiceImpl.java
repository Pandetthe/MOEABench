package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartAlgorithmParameter;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.services.interfaces.AlgorithmRegistryService;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentService;
import pl.edu.agh.to.kotospring.server.services.interfaces.IndicatorRegistryService;
import pl.edu.agh.to.kotospring.server.services.interfaces.ProblemRegistryService;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExperimentServiceImpl implements ExperimentService {
    private final ProblemRegistryService problemRegistry;
    private final AlgorithmRegistryService algorithmRegistry;
    private final IndicatorRegistryService indicatorRegistry;
    private final ExperimentRepository experimentRepository;

    public ExperimentServiceImpl(ProblemRegistryService problemRegistry,
                                 AlgorithmRegistryService algorithmRegistry,
                                 IndicatorRegistryService indicatorRegistry,
                                 ExperimentRepository experimentRepository) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
        this.experimentRepository = experimentRepository;
    }

    @Override
    @Transactional
    public CreateExperimentResponse createExperiment(CreateExperimentRequest request) {
        List<Pair<ExperimentPart, QueueData>> experimentParts = request.stream()
                .map(this::createExperimentPart)
                .toList();

        List<ExperimentPart> experimentPartList = experimentParts.stream()
                .map(Pair::getFirst)
                .toList();

        List<QueueData> queueDataList = experimentParts.stream()
                .map(Pair::getSecond)
                .toList();

        Experiment experiment = new Experiment(OffsetDateTime.now(), experimentPartList);
        for (ExperimentPart part : experimentPartList) {
            part.setExperiment(experiment);
        }
        experimentRepository.save(experiment);

        // TODO: Queue experiment parts

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
                indicators,
                budget
        );
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
        return Optional.empty();
    }

    @Override
    public Optional<GetExperimentStatusResponse> getExperimentStatus(long id) {
        return Optional.empty();
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
        return false;
    }
}
