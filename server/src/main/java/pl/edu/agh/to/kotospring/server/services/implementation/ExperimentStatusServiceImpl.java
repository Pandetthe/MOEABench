package pl.edu.agh.to.kotospring.server.services.implementation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.moeaframework.core.Solution;
import org.moeaframework.core.constraint.Constraint;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.objective.Objective;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.repositories.*;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentStatusService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ExperimentStatusServiceImpl implements ExperimentStatusService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentStatusServiceImpl.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentPartIndicatorRepository experimentPartIndicatorRepository;
    private final ExperimentPartSolutionRepository experimentPartSolutionRepository;
    private final EntityManager entityManager;

    private static final Set<ExperimentStatus> ACTIVE_OR_FINISHED_STATUSES_OF_EXPERIMENT = EnumSet.of(
            ExperimentStatus.IN_PROGRESS,
            ExperimentStatus.SUCCESS,
            ExperimentStatus.PARTIAL_SUCCESS,
            ExperimentStatus.FAILED
    );

    private static final Set<ExperimentRunStatus> ACTIVE_OR_FINISHED_STATUSES_OF_RUN = EnumSet.of(
            ExperimentRunStatus.IN_PROGRESS,
            ExperimentRunStatus.SUCCESS,
            ExperimentRunStatus.PARTIAL_SUCCESS,
            ExperimentRunStatus.FAILED
    );

    public ExperimentStatusServiceImpl(ExperimentRunRepository experimentRunRepository,
                                       ExperimentPartRepository experimentPartRepository,
                                       ExperimentPartIndicatorRepository experimentPartIndicatorRepository,
                                       ExperimentPartSolutionRepository experimentPartSolutionRepository,
                                       ExperimentRepository experimentRepository,
                                       EntityManager entityManager) {
        this.experimentRunRepository = experimentRunRepository;
        this.experimentPartRepository = experimentPartRepository;
        this.experimentPartIndicatorRepository = experimentPartIndicatorRepository;
        this.experimentPartSolutionRepository = experimentPartSolutionRepository;
        this.experimentRepository = experimentRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsStarted(Long partId) {
        ExperimentPart part = getPartOrThrow(partId);
        ExperimentRun experimentRun = part.getExperimentRun();
        OffsetDateTime now = OffsetDateTime.now();
        if (!ACTIVE_OR_FINISHED_STATUSES_OF_RUN.contains(experimentRun.getStatus())) {
            logger.info("Starting run {} of experiment {} triggered by start of part {}", experimentRun.getRunNo(), experimentRun.getExperimentId(), partId);
            experimentRun.setStatus(ExperimentRunStatus.IN_PROGRESS);
            experimentRun.setStartedAt(now);
            Experiment experiment = experimentRun.getExperiment();
            if (!ACTIVE_OR_FINISHED_STATUSES_OF_EXPERIMENT.contains(experiment.getStatus())) {
                logger.info("Starting experiment {} triggered by start of run {} of part {}", experimentRun.getExperimentId(), experimentRun.getRunNo(), partId);
                experiment.setStatus(ExperimentStatus.IN_PROGRESS);
                experiment.setStartedAt(now);
                experimentRepository.saveAndFlush(experiment);
            }
            experimentRunRepository.saveAndFlush(experimentRun);
        }
        part.setStatus(ExperimentPartStatus.RUNNING);
        part.setStartedAt(now);
        experimentPartRepository.saveAndFlush(part);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsCompleted(Long partId, Indicators.IndicatorValues indicatorValues, NondominatedPopulation result) {
        ExperimentPart part = getPartOrThrow(partId);
        processIndicators(part, indicatorValues);
        saveSolutions(part, result);
        part.setStatus(ExperimentPartStatus.COMPLETED);
        part.setFinishedAt(OffsetDateTime.now());
        experimentPartRepository.saveAndFlush(part);
        checkAndFinalizeExperimentRun(part.getExperimentRun().getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsFailed(Long partId, String errorMessage) {
        ExperimentPart part = getPartOrThrow(partId);
        part.setStatus(ExperimentPartStatus.FAILED);
        part.setFinishedAt(OffsetDateTime.now());
        if (errorMessage != null && errorMessage.length() > 2000) {
            errorMessage = errorMessage.substring(0, 2000) + "...";
        }
        part.setErrorMessage(errorMessage);
        experimentPartRepository.saveAndFlush(part);
        checkAndFinalizeExperimentRun(part.getExperimentRun().getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndFinalizeExperimentRun(RunId runId) {
        Experiment experiment = entityManager.find(Experiment.class, runId.getExperimentId(), LockModeType.PESSIMISTIC_WRITE);
        ExperimentRun run = entityManager.find(ExperimentRun.class, runId, LockModeType.PESSIMISTIC_WRITE);
        List<ExperimentPart> parts = experimentPartRepository.findAllByExperimentRunId(runId);
        boolean isStillRunning = parts.stream().anyMatch(p -> p.getStatus() == ExperimentPartStatus.RUNNING || p.getStatus() == ExperimentPartStatus.QUEUED);
        if (isStillRunning) {
            return;
        }
        long totalCount = parts.size();
        long completedCount = parts.stream().filter(p -> p.getStatus() == ExperimentPartStatus.COMPLETED).count();
        OffsetDateTime latestFinished = parts.stream()
                .map(ExperimentPart::getFinishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());
        ExperimentRunStatus finalStatus = determineExperimentRunFinalStatus(totalCount, completedCount);
        run.setStatus(finalStatus);
        run.setFinishedAt(latestFinished);
        experimentRunRepository.saveAndFlush(run);
        logger.info("Run {} of experiment {} finished with status {}", runId.getRunNo(), runId.getExperimentId(), finalStatus);
        internalFinalizeExperiment(experiment);
    }

    private void internalFinalizeExperiment(Experiment experiment) {
        List<ExperimentRun> runs = experimentRunRepository.findAllByIdExperimentId(experiment.getId());
        boolean isStillRunning = runs.stream().anyMatch(r -> r.getStatus() == ExperimentRunStatus.IN_PROGRESS || r.getStatus() == ExperimentRunStatus.QUEUED);
        if (isStillRunning) {
            return;
        }
        long totalCount = runs.size();
        long successfulCount = runs.stream().filter(r -> r.getStatus() == ExperimentRunStatus.SUCCESS).count();
        long partialCount = runs.stream().filter(r -> r.getStatus() == ExperimentRunStatus.PARTIAL_SUCCESS).count();
        OffsetDateTime latestFinished = runs.stream()
                .map(ExperimentRun::getFinishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());
        ExperimentStatus finalStatus = determineExperimentFinalStatus(totalCount, successfulCount, partialCount);
        experiment.setStatus(finalStatus);
        experiment.setFinishedAt(latestFinished);
        experimentRepository.saveAndFlush(experiment);
        logger.info("Experiment {} fully finalized with status {}", experiment.getId(), finalStatus);
    }

    private ExperimentRunStatus determineExperimentRunFinalStatus(long totalCount, long completedCount) {
        if (completedCount == totalCount) {
            return ExperimentRunStatus.SUCCESS;
        } else if (completedCount > 0) {
            return ExperimentRunStatus.PARTIAL_SUCCESS;
        } else {
            return ExperimentRunStatus.FAILED;
        }
    }

    private ExperimentStatus determineExperimentFinalStatus(long totalCount, long successfulCount, long partialCount) {
        if (successfulCount == totalCount) {
            return ExperimentStatus.SUCCESS;
        } else if (successfulCount > 0 || partialCount > 0) {
            return ExperimentStatus.PARTIAL_SUCCESS;
        } else {
            return ExperimentStatus.FAILED;
        }
    }

    private ExperimentPart getPartOrThrow(Long partId) throws IllegalStateException {
        return experimentPartRepository.findById(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPart not found: " + partId));
    }

    private void processIndicators(ExperimentPart part, Indicators.IndicatorValues indicatorValues) {
        Set<ExperimentPartIndicator> existingIndicators = part.getIndicators();
        existingIndicators.forEach(indicator -> {
            String name = indicator.getName();
            StandardIndicator nameParsed = StandardIndicator.valueOf(name);
            double value = indicatorValues.get(nameParsed);
            indicator.setValue(value);
            experimentPartIndicatorRepository.save(indicator);
        });
    }

    private void saveSolutions(ExperimentPart part, NondominatedPopulation result) {
        List<ExperimentPartSolution> solutionEntities = new ArrayList<>();
        for (Solution solution : result) {
            Map<String, String> variablesMap = new HashMap<>();
            Map<String, Double> objectivesMap = new HashMap<>();
            Map<String, Double> constraintsMap = new HashMap<>();
            for (int i = 0; i < solution.getNumberOfVariables(); i++) {
                Variable variable = solution.getVariable(i);
                variablesMap.put(Variable.getNameOrDefault(variable, i), variable.encode());
            }
            for (int i = 0; i < solution.getNumberOfObjectives(); i++) {
                Objective objective = solution.getObjective(i);
                objectivesMap.put(Objective.getNameOrDefault(objective, i), objective.getValue());
            }
            for (int i = 0; i < solution.getNumberOfConstraints(); i++) {
                Constraint constraint = solution.getConstraint(i);
                constraintsMap.put(Constraint.getNameOrDefault(constraint, i), constraint.getValue());
            }
            ExperimentPartSolution solutionEntity = new ExperimentPartSolution(variablesMap, objectivesMap, constraintsMap);
            solutionEntity.setExperimentPart(part);
            solutionEntities.add(solutionEntity);
        }
        if (!solutionEntities.isEmpty()) {
            experimentPartSolutionRepository.saveAll(solutionEntities);
        }
    }
}