package pl.edu.agh.to.kotospring.server.models;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;

public class QueueData {
    private final Long experimentPartId;
    private final Algorithm algorithm;
    private final Indicators indicators;
    private final int budget;

    public QueueData(Long experimentPartId, Algorithm algorithm, Indicators indicators, int budget) {
        this.experimentPartId = experimentPartId;
        this.algorithm = algorithm;
        this.indicators = indicators;
        this.budget = budget;
    }

    public Long getExperimentPartId() {
        return experimentPartId;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public Indicators getIndicators() {
        return indicators;
    }

    public int getBudget() {
        return budget;
    }
}
