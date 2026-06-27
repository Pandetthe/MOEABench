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
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.repositories.*;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentFinalizationService;
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
    private final ExperimentPartExecutionRepository experimentPartExecutionRepository;
    private final ExperimentPartIndicatorRepository experimentPartIndicatorRepository;
    private final ExperimentPartSolutionRepository experimentPartSolutionRepository;
    private final ExperimentFinalizationService experimentFinalizationService;

    private static final Set<ExperimentStatus> ACTIVE_OR_FINISHED_STATUSES_OF_EXPERIMENT = EnumSet.of(
            ExperimentStatus.IN_PROGRESS,
            ExperimentStatus.SUCCESS,
            ExperimentStatus.PARTIAL_SUCCESS,
            ExperimentStatus.FAILED);

    private static final Set<ExperimentRunStatus> ACTIVE_OR_FINISHED_STATUSES_OF_RUN = EnumSet.of(
            ExperimentRunStatus.IN_PROGRESS,
            ExperimentRunStatus.SUCCESS,
            ExperimentRunStatus.PARTIAL_SUCCESS,
            ExperimentRunStatus.FAILED);

    public ExperimentStatusServiceImpl(ExperimentRunRepository experimentRunRepository,
            ExperimentPartExecutionRepository experimentPartExecutionRepository,
            ExperimentPartIndicatorRepository experimentPartIndicatorRepository,
            ExperimentPartSolutionRepository experimentPartSolutionRepository,
            ExperimentRepository experimentRepository,
            ExperimentFinalizationService experimentFinalizationService) {
        this.experimentRunRepository = experimentRunRepository;
        this.experimentPartExecutionRepository = experimentPartExecutionRepository;
        this.experimentPartIndicatorRepository = experimentPartIndicatorRepository;
        this.experimentPartSolutionRepository = experimentPartSolutionRepository;
        this.experimentRepository = experimentRepository;
        this.experimentFinalizationService = experimentFinalizationService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsStarted(Long partId) {
        ExperimentPartExecution part = getPartWithRunAndExperimentOrThrow(partId);
        ExperimentRun experimentRun = part.getExperimentRun();
        OffsetDateTime now = OffsetDateTime.now();
        if (!ACTIVE_OR_FINISHED_STATUSES_OF_RUN.contains(experimentRun.getStatus())) {
            logger.info("Starting run {} of experiment {} triggered by start of part {}", experimentRun.getRunNo(),
                    experimentRun.getExperimentId(), partId);
            experimentRun.setStatus(ExperimentRunStatus.IN_PROGRESS);
            experimentRun.setStartedAt(now);
            Experiment experiment = experimentRun.getExperiment();
            if (!ACTIVE_OR_FINISHED_STATUSES_OF_EXPERIMENT.contains(experiment.getStatus())) {
                logger.info("Starting experiment {} triggered by start of run {} of part {}",
                        experimentRun.getExperimentId(), experimentRun.getRunNo(), partId);
                experiment.setStatus(ExperimentStatus.IN_PROGRESS);
                experiment.setStartedAt(now);
                experimentRepository.saveAndFlush(experiment);
            }
            experimentRunRepository.saveAndFlush(experimentRun);
        }
        part.setStatus(ExperimentPartStatus.RUNNING);
        part.setStartedAt(now);
        experimentPartExecutionRepository.saveAndFlush(part);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsCompleted(Long partId, Indicators.IndicatorValues indicatorValues,
            NondominatedPopulation result, Optional<byte[]> plotImage) {
        ExperimentPartExecution part = getPartWithRunAndExperimentOrThrow(partId);
        processIndicators(part, indicatorValues);
        saveSolutions(part, result);
        part.setPlotImage(plotImage.orElse(null));
        part.setStatus(ExperimentPartStatus.COMPLETED);
        part.setFinishedAt(OffsetDateTime.now());
        experimentPartExecutionRepository.saveAndFlush(part);
        RunId runId = part.getExperimentRun().getId();
        experimentFinalizationService.finalizeRunIfComplete(runId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPartAsFailed(Long partId, String errorMessage) {
        ExperimentPartExecution part = getPartWithRunAndExperimentOrThrow(partId);
        part.setStatus(ExperimentPartStatus.FAILED);
        part.setFinishedAt(OffsetDateTime.now());
        if (errorMessage != null && errorMessage.length() > 2000) {
            errorMessage = errorMessage.substring(0, 2000) + "...";
        }
        part.setErrorMessage(errorMessage);
        experimentPartExecutionRepository.saveAndFlush(part);
        RunId runId = part.getExperimentRun().getId();
        experimentFinalizationService.finalizeRunIfComplete(runId);
    }

    private ExperimentPartExecution getPartWithRunAndExperimentOrThrow(Long partId) {
        return experimentPartExecutionRepository.findByIdWithRunAndExperiment(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPartExecution not found: " + partId));
    }

    private void processIndicators(ExperimentPartExecution part, Indicators.IndicatorValues indicatorValues) {
        part.getIndicators().forEach(indicator -> {
            try {
                StandardIndicator nameParsed = StandardIndicator.valueOf(indicator.getName());
                indicator.setValue(indicatorValues.get(nameParsed));
            } catch (IllegalArgumentException e) {
                logger.warn("Unrecognised indicator name '{}' stored for part {} — value left null",
                        indicator.getName(), part.getId());
            }
            experimentPartIndicatorRepository.save(indicator);
        });
    }

    private void saveSolutions(ExperimentPartExecution part, NondominatedPopulation result) {
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
            ExperimentPartSolution solutionEntity = new ExperimentPartSolution(variablesMap, objectivesMap,
                    constraintsMap);
            solutionEntity.setExperimentPartExecution(part);
            solutionEntities.add(solutionEntity);
        }
        if (!solutionEntities.isEmpty()) {
            experimentPartSolutionRepository.saveAll(solutionEntities);
        }
    }
}
