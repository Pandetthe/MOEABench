package pl.edu.agh.to.kotospring.server.services.implementation;

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
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartIndicator;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartSolution;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartIndicatorRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartSolutionRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentStatusService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ExperimentStatusServiceImpl implements ExperimentStatusService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentStatusServiceImpl.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentPartIndicatorRepository experimentPartIndicatorRepository;
    private final ExperimentPartSolutionRepository experimentPartSolutionRepository;

    private static final Set<ExperimentStatus> ACTIVE_OR_FINISHED_STATUSES = EnumSet.of(
            ExperimentStatus.IN_PROGRESS,
            ExperimentStatus.SUCCESS,
            ExperimentStatus.PARTIAL_SUCCESS,
            ExperimentStatus.FAILED
    );

    public ExperimentStatusServiceImpl(ExperimentRepository experimentRepository,
                                       ExperimentPartRepository experimentPartRepository,
                                       ExperimentPartIndicatorRepository experimentPartIndicatorRepository,
                                       ExperimentPartSolutionRepository experimentPartSolutionRepository) {
        this.experimentRepository = experimentRepository;
        this.experimentPartRepository = experimentPartRepository;
        this.experimentPartIndicatorRepository = experimentPartIndicatorRepository;
        this.experimentPartSolutionRepository = experimentPartSolutionRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsStarted(Long partId) {
        ExperimentPart part = getPartOrThrow(partId);
        Experiment experiment = part.getExperiment();
        OffsetDateTime now = OffsetDateTime.now();
        if (!ACTIVE_OR_FINISHED_STATUSES.contains(experiment.getStatus())) {
            logger.info("Starting Experiment {} triggered by start of Part {}", experiment.getId(), partId);
            experiment.setStatus(ExperimentStatus.IN_PROGRESS);
            if (experiment.getStartedAt() == null) {
                experiment.setStartedAt(now);
            }
            experimentRepository.save(experiment);
        }

        part.setStatus(ExperimentPartStatus.RUNNING);
        part.setStartedAt(now);
        experimentPartRepository.save(part);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsCompleted(Long partId, Indicators.IndicatorValues indicatorValues, NondominatedPopulation result) {
        ExperimentPart part = getPartOrThrow(partId);

        processIndicators(part, indicatorValues);
        saveSolutions(part, result);


        part.setStatus(ExperimentPartStatus.COMPLETED);
        part.setFinishedAt(OffsetDateTime.now());
        experimentPartRepository.save(part);

        checkAndFinalizeExperiment(part.getExperiment());
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
        experimentPartRepository.save(part);

        checkAndFinalizeExperiment(part.getExperiment());
    }

    private void checkAndFinalizeExperiment(Experiment experiment) {
        Set<ExperimentPart> parts = experiment.getParts();

        boolean anyRunningOrPending = parts.stream()
                .anyMatch(part -> part.getStatus() == ExperimentPartStatus.RUNNING
                        || part.getStatus() == ExperimentPartStatus.QUEUED);

        if (anyRunningOrPending) {
            return;
        }

        long totalCount = parts.size();
        long completedCount = parts.stream()
                .filter(part -> part.getStatus() == ExperimentPartStatus.COMPLETED)
                .count();
        long failedCount = parts.stream()
                .filter(part -> part.getStatus() == ExperimentPartStatus.FAILED)
                .count();

        OffsetDateTime latestFinished = parts.stream()
                .map(ExperimentPart::getFinishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());

        ExperimentStatus finalStatus = determineFinalStatus(totalCount, completedCount);

        experiment.setStatus(finalStatus);
        experiment.setFinishedAt(latestFinished);
        experimentRepository.save(experiment);

        logger.info("Experiment {} finished with status {} (Completed: {}, Failed: {}, Total: {})",
                experiment.getId(), finalStatus, completedCount, failedCount, totalCount);
    }

    private ExperimentStatus determineFinalStatus(long totalCount, long completedCount) {
        if (completedCount == totalCount) {
            return ExperimentStatus.SUCCESS;
        } else if (completedCount > 0) {
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

            // If this method throws any error, we should abort whole result saving
            // because this state should not happen and must be fixed ASAP
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

            ExperimentPartSolution solutionEntity = new ExperimentPartSolution(part, variablesMap, objectivesMap, constraintsMap);
            solutionEntities.add(solutionEntity);
        }

        if (!solutionEntities.isEmpty()) {
            experimentPartSolutionRepository.saveAll(solutionEntities);
        }
    }
}