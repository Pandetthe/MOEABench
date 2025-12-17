package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartAlgorithmParameter;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartIndicator;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.AlgorithmRegistryService;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentService;
import pl.edu.agh.to.kotospring.server.services.interfaces.IndicatorRegistryService;
import pl.edu.agh.to.kotospring.server.services.interfaces.ProblemRegistryService;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
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
    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentExecutionServiceImpl executionService;

    public ExperimentServiceImpl(ProblemRegistryService problemRegistry,
                                 AlgorithmRegistryService algorithmRegistry,
                                 IndicatorRegistryService indicatorRegistry,
                                 ExperimentRepository experimentRepository, ExperimentPartRepository experimentPartRepository, ExperimentExecutionServiceImpl executionService) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
        this.experimentRepository = experimentRepository;
        this.experimentPartRepository = experimentPartRepository;
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

        experiment.setStatus(ExperimentStatus.IN_PROGRESS);
        experiment.setStartedAt(OffsetDateTime.now());
        experimentRepository.save(experiment);
        logger.info("Created experiment with ID: {}", experiment.getId());


        for (int i = 0; i < experimentPartList.size(); i++) {
            logger.info("experiment part size: {}", experimentPartList.size());
            ExperimentPart savedPart = experimentPartList.get(i);
            logger.info("ExperimentPart {} has ID: {}", i, savedPart.getId());

            QueueData originalQueueData = queueDataList.get(i);

            QueueData readyToRunData = new QueueData(
                    savedPart.getId(),
                    originalQueueData.algorithm(),
                    originalQueueData.indicators(),
                    originalQueueData.budget()
            );


            executionService.partStatusManager(readyToRunData);
            logger.info("after runExperimentPart");

        }
        logger.info("Created experiment with id {}", experiment.getId());
        return new CreateExperimentResponse(experiment.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndUpdateExperimentStatus(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalStateException("Experiment not found: " + experimentId));

        Set<ExperimentPart> parts = experiment.getParts();

        boolean allFinished = parts.stream()
                .allMatch(part -> part.getStatus() == ExperimentPartStatus.COMPLETED
                        || part.getStatus() == ExperimentPartStatus.FAILED);

        if (!allFinished) {
            logger.info("Experiment {} still has running parts", experimentId);
            return;
        }
        long completedCount = parts.stream()
                .filter(part -> part.getStatus() == ExperimentPartStatus.COMPLETED)
                .count();
        long failedCount = parts.stream()
                .filter(part -> part.getStatus() == ExperimentPartStatus.FAILED)
                .count();
        long totalCount = parts.size();

        ExperimentStatus finalStatus;
        if (completedCount == totalCount) {
            finalStatus = ExperimentStatus.SUCCESS;
        } else if (completedCount > 0) {
            finalStatus = ExperimentStatus.PARTIAL_SUCCESS;
        } else {
            finalStatus = ExperimentStatus.FAILED;
        }

        experiment.setStatus(finalStatus);
        experiment.setFinishedAt(OffsetDateTime.now());
        experimentRepository.save(experiment);

        logger.info("Experiment {} finished with status {} (completed: {}/{}, failed: {}/{})",
                experimentId, finalStatus, completedCount, totalCount, failedCount, totalCount);
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Optional<GetExperimentResponse> getExperiment(long id) {
        return experimentRepository.findWithPartsById(id)
                .map(experiment -> new GetExperimentResponse(
                        experiment.getStatus(),
                        experiment.getQueuedAt(),
                        experiment.getStartedAt(),
                        experiment.getFinishedAt(),
                        experiment.getParts().stream().map(part -> new GetExperimentResponseData(
                                part.getId(),
                                part.getStatus(),
                                part.getErrorMessage(),
                                part.getProblem(),
                                part.getAlgorithm(),
                                part.getBudget(),
                                part.getParameters().stream().collect(Collectors.toMap(
                                        ExperimentPartAlgorithmParameter::getKey,
                                        ExperimentPartAlgorithmParameter::getValue,
                                        (existing, replacement) -> replacement,
                                        HashMap::new)),
                                new HashSet<>(part.getIndicators().stream().map(ExperimentPartIndicator::getName).toList()),
                                part.getStartedAt(),
                                part.getFinishedAt()
                        )).toList()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GetExperimentStatusResponse> getExperimentStatus(long id) {
        return experimentRepository.findById(id)
                .map(experiment -> new GetExperimentStatusResponse(
                        experiment.getStatus()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GetExperimentPartStatusResponse> getExperimentStatus(long id, long partId) {
        return experimentPartRepository.findByExperimentIdAndId(id, partId)
                .map(part -> new GetExperimentPartStatusResponse(
                        part.getStatus(),
                        part.getErrorMessage()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GetExperimentResultResponse> getExperimentResult(long id) {
        return experimentRepository.findWithFullSolutionById(id)
                .map(experiment ->
                        experiment.getParts().stream()
                                .filter(part -> part.getStatus() == ExperimentPartStatus.COMPLETED)
                                .map(part ->
                                        new GetExperimentResultResponseData(
                                                part.getId(),
                                                part.getSolutions().stream()
                                                        .map(solution -> new AlgorithmResult(
                                                                solution.getVariables(),
                                                                solution.getObjectives(),
                                                                solution.getConstraints()
                                                        ))
                                                        .collect(Collectors.toList()),
                                                part.getIndicators().stream()
                                                        .collect(Collectors.toMap(
                                                                ExperimentPartIndicator::getName,
                                                                ExperimentPartIndicator::getValue
                                                        ))
                                        )
                                )
                                .collect(Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        GetExperimentResultResponse::new
                                ))
                );

    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GetExperimentPartResultResponse> getExperimentResult(long id, long partId) {
        return experimentPartRepository.findWithFullSolutionById(id, partId)
                .map(part -> {
                    if (part.getStatus() != ExperimentPartStatus.COMPLETED) {
                        throw new NotFoundException();
                    }
                    List<AlgorithmResult> results = part.getSolutions().stream()
                            .map(solution -> new AlgorithmResult(
                                    solution.getVariables(),
                                    solution.getObjectives(),
                                    solution.getConstraints()
                            ))
                            .collect(Collectors.toList());

                    Map<String, Double> indicatorsValues = part.getIndicators().stream()
                            .collect(Collectors.toMap(
                                    ExperimentPartIndicator::getName,
                                    ExperimentPartIndicator::getValue
                            ));

                    return new GetExperimentPartResultResponse(results, indicatorsValues);
                });
    }

    @Override
    @Transactional
    public boolean deleteExperiment(long id) {
        if(experimentRepository.existsById(id)){
            experimentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
