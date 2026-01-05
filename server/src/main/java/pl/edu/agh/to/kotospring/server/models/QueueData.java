package pl.edu.agh.to.kotospring.server.models;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;

public class QueueData {
    private Long experimentPartId;
    private final Algorithm algorithm;
    private final Indicators indicators;
    private final int budget;

    public QueueData(Algorithm algorithm, Indicators indicators, int budget) {
        this.algorithm = algorithm;
        this.indicators = indicators;
        this.budget = budget;
    }

    public Long getExperimentPartId() {
        return this.experimentPartId;
    }

    public void setExperimentPartId(Long experimentPartId) {
        this.experimentPartId = experimentPartId;
    }

    public Algorithm getAlgorithm() {
        return this.algorithm;
    }

    public Indicators getIndicators() {
        return this.indicators;
    }

    public int getBudget() {
        return this.budget;
    }
}
