package pl.edu.agh.to.kotospring.server.services.interfaces;

import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;

import java.util.Optional;

public interface ExperimentStatusService {
    void markPartAsStarted(Long partId);

    void markPartAsCompleted(Long partId, Indicators.IndicatorValues indicatorValues, NondominatedPopulation result,
            Optional<byte[]> plotImage);

    void markPartAsFailed(Long partId, String errorMessage);
}