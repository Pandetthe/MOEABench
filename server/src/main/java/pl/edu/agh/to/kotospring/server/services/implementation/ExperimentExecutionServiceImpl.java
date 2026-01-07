package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartExecutionRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentExecutionService;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentStatusService;

@Service
public class ExperimentExecutionServiceImpl implements ExperimentExecutionService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentExecutionServiceImpl.class);

    private final ExperimentPartExecutionRepository experimentPartExecutionRepository;
    private final ExperimentStatusService experimentStatusService;

    public ExperimentExecutionServiceImpl(ExperimentPartExecutionRepository experimentPartExecutionRepository,
            ExperimentStatusService experimentStatusService) {
        this.experimentPartExecutionRepository = experimentPartExecutionRepository;
        this.experimentStatusService = experimentStatusService;
    }

    @Override
    @Async("experimentExecutor")
    public void enqueue(QueueData queueData) {
        Long partId = queueData.getExperimentPartId();
        logger.info("Execution starting for ExperimentPart {}", partId);

        if (!experimentPartExecutionRepository.existsById(partId)) {
            logger.error("ExperimentPartExecution {} does not exist! Aborting...", partId);
            return;
        }

        try {
            experimentStatusService.markPartAsStarted(partId);

            Algorithm algorithm = queueData.getAlgorithm();
            int budget = queueData.getBudget();
            Indicators indicators = queueData.getIndicators();
            algorithm.run(budget);
            NondominatedPopulation result = algorithm.getResult();
            Indicators.IndicatorValues indicatorValues = indicators.apply(result);

            experimentStatusService.markPartAsCompleted(partId, indicatorValues, result);
            logger.info("Finished execution of ExperimentPart ID {}", partId);

        } catch (Exception e) {
            logger.error("Error executing ExperimentPart ID {}", partId, e);
            experimentStatusService.markPartAsFailed(partId, e.getMessage());
        }
    }
}