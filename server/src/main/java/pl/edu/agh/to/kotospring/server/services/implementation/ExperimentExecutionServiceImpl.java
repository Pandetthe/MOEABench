package pl.edu.agh.to.kotospring.server.services.implementation;

import org.jfree.chart.JFreeChart;
import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.algorithm.extension.Frequency;
import org.moeaframework.analysis.plot.XYPlotBuilder;
import org.moeaframework.analysis.runtime.InstrumentedAlgorithm;
import org.moeaframework.analysis.runtime.Instrumenter;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.models.QueueData;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartExecutionRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentExecutionService;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentStatusService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Optional;

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
            Collection<StandardIndicator> selectedIndicators = indicators.getSelectedIndicators();

            Instrumenter instrumenter = new Instrumenter()
                    .withFrequency(Frequency.ofEvaluations(100))
                    .withReferenceSet(queueData.getReferenceSet());

            attachCollectors(instrumenter, selectedIndicators);

            InstrumentedAlgorithm<Algorithm> instrumentedAlgorithm = instrumenter.instrument(algorithm);
            instrumentedAlgorithm.run(budget);

            NondominatedPopulation result = instrumentedAlgorithm.getResult();
            Indicators.IndicatorValues indicatorValues = indicators.apply(result);

            Optional<byte[]> plotImage = generatePlotImage(instrumentedAlgorithm, selectedIndicators);

            experimentStatusService.markPartAsCompleted(partId, indicatorValues, result, plotImage);
            logger.info("Finished execution of ExperimentPart ID {}", partId);

        } catch (Exception e) {
            logger.error("Error executing ExperimentPart ID {}", partId, e);
            experimentStatusService.markPartAsFailed(partId, e.getMessage());
        }
    }

    private Optional<byte[]> generatePlotImage(InstrumentedAlgorithm<Algorithm> instrumentedAlgorithm,
            Collection<StandardIndicator> selectedIndicators) {
        if (selectedIndicators.isEmpty()) {
            return Optional.empty();
        }

        try {
            JFreeChart chart = new XYPlotBuilder()
                    .lines(instrumentedAlgorithm.getSeries())
                    .build();

            BufferedImage image = chart.createBufferedImage(800, 600);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Optional.of(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to generate plot image", e);
            return Optional.empty();
        }
    }

    private void attachCollectors(Instrumenter instrumenter, Collection<StandardIndicator> selectedIndicators) {
        for (StandardIndicator indicator : selectedIndicators) {
            switch (indicator) {
                case Hypervolume -> instrumenter.attachHypervolumeCollector();
                case GenerationalDistance -> instrumenter.attachGenerationalDistanceCollector();
                case GenerationalDistancePlus -> instrumenter.attachGenerationalDistancePlusCollector();
                case InvertedGenerationalDistance -> instrumenter.attachInvertedGenerationalDistanceCollector();
                case InvertedGenerationalDistancePlus -> instrumenter.attachInvertedGenerationalDistancePlusCollector();
                case Spacing -> instrumenter.attachSpacingCollector();
                case AdditiveEpsilonIndicator -> instrumenter.attachAdditiveEpsilonIndicatorCollector();
                case Contribution -> instrumenter.attachContributionCollector();
                case MaximumParetoFrontError -> instrumenter.attachMaximumParetoFrontErrorCollector();
                case R1Indicator -> instrumenter.attachR1Collector();
                case R2Indicator -> instrumenter.attachR2Collector();
                case R3Indicator -> instrumenter.attachR3Collector();
                default -> {}
            }
        }
    }
}