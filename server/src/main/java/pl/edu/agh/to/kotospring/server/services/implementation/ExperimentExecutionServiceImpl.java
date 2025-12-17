package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.core.Solution;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartIndicator;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartSolution;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartIndicatorRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartSolutionEntityRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentExecutionService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ExperimentExecutionServiceImpl implements ExperimentExecutionService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentExecutionServiceImpl.class);
    private final ExperimentPartRepository experimentPartRepository;
    private final ExperimentPartIndicatorRepository experimentPartIndicatorRepository;
    private final ExperimentPartSolutionEntityRepository experimentPartSolutionEntityRepository;
    private ExperimentExecutionServiceImpl self;
    private ExperimentServiceImpl experimentService;

    public ExperimentExecutionServiceImpl(ExperimentPartRepository experimentPartRepository,
                                          ExperimentPartIndicatorRepository experimentPartIndicatorRepository,
                                          ExperimentPartSolutionEntityRepository experimentPartSolutionEntityRepository,
                                          @Lazy ExperimentExecutionServiceImpl self,
                                          @Lazy ExperimentServiceImpl experimentService) {
        this.experimentPartRepository = experimentPartRepository;
        this.experimentPartIndicatorRepository = experimentPartIndicatorRepository;
        this.experimentPartSolutionEntityRepository = experimentPartSolutionEntityRepository;
        this.self = self;
        this.experimentService = experimentService;
    }


//    public void setSelf(ExperimentExecutionService self) {
//        this.self = self;
//    }

    @Async("threadPoolTaskExecutor")
    public void partStatusManager(QueueData queueData) {
        Long partId = queueData.experimentPartId();
        logger.info("Starting execution of ExperimentPart ID: {}", partId);
        Long experimentId = experimentPartRepository.findById(partId)
                .map(part -> part.getExperiment().getId())
                .orElse(null);
        self.updatePartStatus(partId, ExperimentPartStatus.RUNNING, OffsetDateTime.now(), null);

        try {
            self.runExperimentPart(queueData);

            self.updatePartStatus(partId, ExperimentPartStatus.COMPLETED, null, OffsetDateTime.now());
            logger.info("Finished execution of ExperimentPart ID: {}", partId);
        } catch (Exception e) {
            logger.error("Error executing ExperimentPart ID: " + partId, e);
            self.errorPartStatus(partId, ExperimentPartStatus.FAILED, e.getMessage(), OffsetDateTime.now());
        } finally {
            if (experimentId != null) {
                experimentService.checkAndUpdateExperimentStatus(experimentId);
            }
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
        experimentPartRepository.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void errorPartStatus(Long partId, ExperimentPartStatus status, String errorMessage, OffsetDateTime finishedAt) {
        ExperimentPart part = experimentPartRepository.findById(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPart not found: " + partId));

        part.setStatus(status);
        part.setErrorMessage(errorMessage);
        part.setFinishedAt(finishedAt);

        experimentPartRepository.save(part);
        experimentPartRepository.flush();
    }

    @Transactional
    public void runExperimentPart(QueueData queueData) {
        Long partId = queueData.experimentPartId();

        ExperimentPart part = experimentPartRepository.findById(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPart not found: " + partId));

        part.setStartedAt(OffsetDateTime.now());
        part.setStatus(ExperimentPartStatus.RUNNING);
        experimentPartRepository.save(part);




//            Enumeration<Integer> populationSize = Parameter.named("populationSize")
//                    .asInt()
//                    .range(10, 100, 10);
//
//            Enumeration<Long> seed = org.moeaframework.analysis.parameter.Parameter.named("seed")
//                    .asLong()
//                    .random(0, Long.MAX_VALUE, 10);
//
//            ParameterSet parameters = new ParameterSet(populationSize, seed);
//
//            Samples samples = parameters.enumerate();

            var algorithm = queueData.algorithm();
            int maxEvaluations = queueData.budget();

            algorithm.run(maxEvaluations);
            NondominatedPopulation result = algorithm.getResult();
            result.display();
            logger.info("result: {}", result);
            Indicators moeaIndicators = queueData.indicators();
            Indicators.IndicatorValues indires = moeaIndicators.apply(result);
            logger.info("indi: {}", moeaIndicators);
            List<ExperimentPartIndicator> indicatorsList = new ArrayList<>();

            logger.info("test: {}", result.size());

            EnumSet<StandardIndicator> indicators = moeaIndicators.getSelectedIndicators();
//            indicators.forEach(indicator -> {
//                String name = indicator.name();
//                double value = indires.get(indicator);
//                logger.info("Indicator {}: {}", name, value);
//                ExperimentPartIndicator experimentPartIndicator = new ExperimentPartIndicator(name, value);
//                experimentPartIndicator.setExperimentPart(part);
//                indicatorsList.add(experimentPartIndicator);
//                experimentPartIndicatorRepository.save(experimentPartIndicator);
//
//            });

            List<ExperimentPartIndicator> existingIndicators = part.getIndicators();
            indicators.forEach(indicator -> {
                String name = indicator.name();
                double value = indires.get(indicator);

                logger.info("Updating Indicator {}: {}", name, value);

                existingIndicators.stream()
                        .filter(existing -> existing.getName().equalsIgnoreCase(name))
                        .findFirst()
                        .ifPresentOrElse(
                                existingInd -> {
                                    existingInd.setValue(value);

                                    experimentPartIndicatorRepository.save(existingInd);
                                },
                                () -> {
                                    ExperimentPartIndicator newInd = new ExperimentPartIndicator(name, value);
                                    newInd.setExperimentPart(part);
                                    existingIndicators.add(newInd);
                                    experimentPartIndicatorRepository.save(newInd);
                                }
                        );
            });


            List<ExperimentPartSolution> solutionEntities = new ArrayList<>();

            for (Solution solution: result) {

                Map<String, String> variablesMap = new HashMap<>();
                Map<String, Double> objectivesMap = new HashMap<>();
                Map<String, Double> constraintsMap = new HashMap<>();

                for (int i = 0; i < solution.getNumberOfVariables(); i++) {
                    String key = "var_" + i;
                    variablesMap.put(key, solution.getVariable(i).encode());
                }


                for (int i = 0; i < solution.getNumberOfObjectives(); i++) {
                    String key = "obj_" + i;
                    objectivesMap.put(key, solution.getObjectiveValue(i));
                }

                for (int i = 0; i < solution.getNumberOfConstraints(); i++) {
                    String key = "const_" + i;
                    constraintsMap.put(key, solution.getConstraintValue(i));
                }
                ExperimentPartSolution solutionEntity = new ExperimentPartSolution(part, variablesMap, objectivesMap, constraintsMap);
                solutionEntities.add(solutionEntity);
            }

//            experimentPartSolutionEntityRepository.saveAll(solutionEntities);
//            part.setSolutionEntities(solutionEntities);

            part.getSolutionEntities().clear();
            part.getSolutionEntities().addAll(solutionEntities);
            experimentPartSolutionEntityRepository.saveAll(solutionEntities);


//            if (moeaIndicators != null && part.getIndicators() != null) {
//                for (ExperimentPartIndicator partIndicatorEntity : part.getIndicators()) {
//                    try {
//                        logger.info("name: {}", partIndicatorEntity.getName());
//                        logger.info("indicators: {}", moeaIndicators);
//                        logger.info("result: {}", result);
//
//                        Indicators.IndicatorValues indicatorValues = moeaIndicators.apply(result);
//                        indicatorValues.display();
//
//                    } catch (Exception e) {
//                        logger.warn("Could not calculate indicator '{}': {}",
//                                partIndicatorEntity.getName(), e.getMessage());
////                        partIndicatorEntity.setValue(-1);
//                    }
//                }
//            }

//            part.setStatus(ExperimentPartStatus.COMPLETED);
//            part.setFinishedAt(OffsetDateTime.now());
//            logger.info("Finished execution of ExperimentPart ID: {}", partId);
//
//        } catch (Exception e) {
//            logger.error("Error executing ExperimentPart ID: " + partId, e);
//            part.setStatus(ExperimentPartStatus.FAILED);
//            part.setErrorMessage(e.getMessage());
//            part.setFinishedAt(OffsetDateTime.now());
//
//        } finally {
////            logger.info("getting here?");
//            experimentPartRepository.save(part);
////            logger.info("and here?");
//        }
    }

}