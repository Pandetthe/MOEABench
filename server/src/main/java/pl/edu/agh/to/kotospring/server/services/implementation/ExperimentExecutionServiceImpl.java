package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.Solution;
import org.moeaframework.core.constraint.Constraint;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.objective.Objective;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartIndicator;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartSolution;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartIndicatorRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartSolutionRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentExecutionService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ExperimentExecutionServiceImpl implements ExperimentExecutionService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentExecutionServiceImpl.class);

    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentPartIndicatorRepository experimentPartIndicatorRepository;
    private final ExperimentPartSolutionRepository experimentPartSolutionRepository;
    private final ExperimentRepository experimentRepository;

    // Self-injection to allow calling @Transactional methods from within the same class
    private final ExperimentExecutionServiceImpl self;

    public ExperimentExecutionServiceImpl(ExperimentPartRepository experimentPartRepository,
                                          ExperimentPartIndicatorRepository experimentPartIndicatorRepository,
                                          ExperimentPartSolutionRepository experimentPartSolutionRepository,
                                          ExperimentRepository experimentRepository,
                                          @Lazy ExperimentExecutionServiceImpl self) {
        this.experimentPartRepository = experimentPartRepository;
        this.experimentPartIndicatorRepository = experimentPartIndicatorRepository;
        this.experimentPartSolutionRepository = experimentPartSolutionRepository;
        this.experimentRepository = experimentRepository;
        this.self = self;
    }

    @Override
    @Async("experimentExecutor")
    public void enqueue(QueueData queueData) {
        Long partId = queueData.getExperimentPartId();
        logger.info("Manager starting for ExperimentPart ID: {}", partId);

        Long experimentId = experimentPartRepository.findById(partId)
                .map(part -> part.getExperiment().getId())
                .orElse(null);

        if (experimentId == null) {
            logger.error("ExperimentPart {} has no associated Experiment. Aborting.", partId);
            return;
        }

        try {
            self.markExperimentAsStartedIfNecessary(experimentId);

            self.updatePartStatus(partId, ExperimentPartStatus.RUNNING, OffsetDateTime.now(), null);

            self.runExperimentPart(queueData);

            self.updatePartStatus(partId, ExperimentPartStatus.COMPLETED, null, OffsetDateTime.now());
            logger.info("Finished execution of ExperimentPart ID: {}", partId);

        } catch (Exception e) {
            logger.error("Error executing ExperimentPart ID: {}", partId, e);
            self.errorPartStatus(partId, ExperimentPartStatus.FAILED, e.getMessage(), OffsetDateTime.now());
        } finally {
            self.checkAndUpdateExperimentStatus(experimentId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExperimentAsStartedIfNecessary(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalStateException("Experiment not found: " + experimentId));

        if (experiment.getStatus() != ExperimentStatus.IN_PROGRESS &&
                experiment.getStatus() != ExperimentStatus.SUCCESS &&
                experiment.getStatus() != ExperimentStatus.PARTIAL_SUCCESS &&
                experiment.getStatus() != ExperimentStatus.FAILED) {

            logger.info("Marking Experiment {} as IN_PROGRESS", experimentId);
            experiment.setStatus(ExperimentStatus.IN_PROGRESS);
            if (experiment.getStartedAt() == null) {
                experiment.setStartedAt(OffsetDateTime.now());
            }
            experimentRepository.save(experiment);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePartStatus(Long partId, ExperimentPartStatus status, OffsetDateTime startedAt, OffsetDateTime finishedAt) {
        ExperimentPart part = experimentPartRepository.findById(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPart not found: " + partId));

        part.setStatus(status);
        if (startedAt != null) {
            part.setStartedAt(startedAt);
        }
        if (finishedAt != null) {
            part.setFinishedAt(finishedAt);
        }

        experimentPartRepository.save(part);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void errorPartStatus(Long partId, ExperimentPartStatus status, String errorMessage, OffsetDateTime finishedAt) {
        ExperimentPart part = experimentPartRepository.findById(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPart not found: " + partId));

        part.setStatus(status);
        if (errorMessage != null && errorMessage.length() > 2000) {
            errorMessage = errorMessage.substring(0, 2000) + "...";
        }
        part.setErrorMessage(errorMessage);
        part.setFinishedAt(finishedAt);

        experimentPartRepository.save(part);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndUpdateExperimentStatus(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalStateException("Experiment not found: " + experimentId));

        Set<ExperimentPart> parts = experiment.getParts();

        boolean anyRunningOrPending = parts.stream()
                .anyMatch(part -> part.getStatus() == ExperimentPartStatus.RUNNING
                        || part.getStatus() == ExperimentPartStatus.QUEUED);

        if (anyRunningOrPending) {
            return;
        }

        // Calculate statistics
        long totalCount = parts.size();
        long completedCount = parts.stream()
                .filter(part -> part.getStatus() == ExperimentPartStatus.COMPLETED)
                .count();
        long failedCount = parts.stream()
                .filter(part -> part.getStatus() == ExperimentPartStatus.FAILED)
                .count();

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

        logger.info("Experiment {} finished with status {} (Completed: {}, Failed: {}, Total: {})",
                experimentId, finalStatus, completedCount, failedCount, totalCount);
    }

    @Transactional
    public void runExperimentPart(QueueData queueData) {
        Long partId = queueData.getExperimentPartId();
        ExperimentPart part = experimentPartRepository.findById(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPart not found: " + partId));

        Algorithm algorithm = queueData.getAlgorithm();
        int budget = queueData.getBudget();
        algorithm.run(budget);

        NondominatedPopulation result = algorithm.getResult();

        processIndicators(part, queueData.getIndicators(), result);

        saveSolutions(part, result);
    }

    private void processIndicators(ExperimentPart part, Indicators moeaIndicators, NondominatedPopulation result) {
        if (moeaIndicators == null) return;

        Indicators.IndicatorValues indicatorValues = moeaIndicators.apply(result);
        EnumSet<StandardIndicator> indicators = moeaIndicators.getSelectedIndicators();
        Set<ExperimentPartIndicator> existingIndicators = part.getIndicators();

        indicators.forEach(indicator -> {
            String name = indicator.name();
            double value = indicatorValues.get(indicator);

            Optional<ExperimentPartIndicator> existingOpt = existingIndicators.stream()
                    .filter(existing -> existing.getName().equalsIgnoreCase(name))
                    .findFirst();

            if (existingOpt.isPresent()) {
                ExperimentPartIndicator existingInd = existingOpt.get();
                existingInd.setValue(value);
                experimentPartIndicatorRepository.save(existingInd);
            } else {
                ExperimentPartIndicator newInd = new ExperimentPartIndicator(name, value);
                newInd.setExperimentPart(part);
                experimentPartIndicatorRepository.save(newInd);
                existingIndicators.add(newInd);
            }
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