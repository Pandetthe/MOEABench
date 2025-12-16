package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.analysis.parameter.Enumeration;
import org.moeaframework.analysis.parameter.Parameter;
import org.moeaframework.analysis.parameter.ParameterSet;
import org.moeaframework.analysis.sample.SampledResults;
import org.moeaframework.analysis.sample.Samples;
import org.moeaframework.analysis.stream.Groupings;
import org.moeaframework.analysis.stream.Measures;
import org.moeaframework.analysis.stream.Partition;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.indicator.Hypervolume;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.core.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartIndicator;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.IndicatorRegistryService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ExperimentExecutionService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentExecutionService.class);
    private final ExperimentPartRepository experimentPartRepository;
    private final IndicatorRegistryService indicatorRegistryService;

    public ExperimentExecutionService(ExperimentPartRepository experimentPartRepository, IndicatorRegistryService indicatorRegistryService) {
        this.experimentPartRepository = experimentPartRepository;
        this.indicatorRegistryService = indicatorRegistryService;
    }

    @Async("threadPoolTaskExecutor")
    @Transactional
    public void runExperimentPart(QueueData queueData) {
        Long partId = queueData.getExperimentPartId();
        logger.info("Starting execution of ExperimentPart ID: {}", partId);

        ExperimentPart part = experimentPartRepository.findById(partId)
                .orElseThrow(() -> new IllegalStateException("ExperimentPart not found: " + partId));

        part.setStartedAt(OffsetDateTime.now());
        part.setStatus(ExperimentPartStatus.RUNNING);
        experimentPartRepository.save(part);

        try {

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
//
//            SampledResults<NondominatedPopulation> results = samples.evaluateAll(sample -> {
//                PRNG.setSeed(sample.getLong("seed"));
//
//                var algorithm = queueData.getAlgorithm();
//                algorithm.applyConfiguration(sample);
//                algorithm.run(10000);
//
//                return algorithm.getResult();
//            });
            var algorithm = queueData.getAlgorithm();
            int maxEvaluations = queueData.getBudget();

            algorithm.run(maxEvaluations);
            NondominatedPopulation result = algorithm.getResult();
            result.display();
            logger.info("result: {}", result);
            Indicators indicatorsList = queueData.getIndicators();
            EnumSet<StandardIndicator> indicatorNames = indicatorsList.getSelectedIndicators();

            Indicators moeaIndicators = indicatorRegistryService.getIndicators(indicatorNames, algorithm.getProblem(), result);

//            NondominatedPopulation referenceSet = NondominatedPopulation.load("./pf/DTLZ2.3D.pf");
//            Indicators indicators = Indicators.all(algorithm.getProblem(), referenceSet);
//            Hypervolume hypervolume = new Hypervolume(algorithm.getProblem(), referenceSet);
//            hypervolume.evaluate(result);
//            Indicators.IndicatorValues indicatorValues = indicators.apply(result);
//            indicatorValues.display();
//            logger.info("Hypervolume: {}", hypervolume.evaluate(result));

//            Indicators.IndicatorValues indicatorValues = moeaIndicators.apply(result);
//            indicatorValues.display();

//            Indicators indicators = indicatorRegistryService.getIndicators(moeaIndicators, algorithm.getProblem(), result);


            if (moeaIndicators != null && part.getIndicators() != null) {
                for (ExperimentPartIndicator partIndicatorEntity : part.getIndicators()) {
                    try {
                        logger.info("name: {}", partIndicatorEntity.getName());
                        logger.info("indicators: {}", moeaIndicators);
                        logger.info("result: {}", result);

                        Indicators.IndicatorValues indicatorValues = moeaIndicators.apply(result);
                        indicatorValues.display();


//                        Hypervolume hypervolume = new Hypervolume(algorithm.getProblem(), NondominatedPopulation.load("./pf/DTLZ2.2D.pf"));
//                        hypervolume.evaluate(result);
//                        hypervolume.

//                        Partition<Integer, Double> avgHypervolume = result
//                                .stream()
//                                .map(hypervolume::evaluate)
//                                .groupBy(Groupings.exactValue(populationSize))
//                                .measureEach(Measures.average())
//                                .sorted();
//
//                        avgHypervolume.display();

                    } catch (Exception e) {
                        logger.warn("Could not calculate indicator '{}': {}",
                                partIndicatorEntity.getName(), e.getMessage());
//                        partIndicatorEntity.setValue(-1);
                    }
                }
            }

//            saveSolutionsToDatabase(part, result);

            part.setStatus(ExperimentPartStatus.COMPLETED);
            part.setFinishedAt(OffsetDateTime.now());
            logger.info("Finished execution of ExperimentPart ID: {}", partId);

        } catch (Exception e) {
            logger.error("Error executing ExperimentPart ID: " + partId, e);
            part.setStatus(ExperimentPartStatus.FAILED);
            part.setErrorMessage(e.getMessage());
            part.setFinishedAt(OffsetDateTime.now());
        } finally {
            experimentPartRepository.save(part);
        }
    }

    private void saveSolutionsToDatabase(ExperimentPart part, NondominatedPopulation population) {
        for (Solution solution : population) {

//            Map<String, Double> objectiveMap = new HashMap<>();
//            for (int i = 0; i < solution.getNumberOfObjectives(); i++) {
//                String key = "obj_" + i;
//                objectiveMap.put(key, solution.getObjective(i));
//            }
//
//            Map<String, Double> constraintMap = new HashMap<>();
//            for (int i = 0; i < solution.getNumberOfConstraints(); i++) {
//                String key = "const_" + i;
//                constraintMap.put(key, solution.getConstraint(i));
//            }
        }
    }
}