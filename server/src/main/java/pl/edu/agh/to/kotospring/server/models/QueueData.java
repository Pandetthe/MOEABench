package pl.edu.agh.to.kotospring.server.models;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;

public class QueueData {
    private Long experimentPartId;
    private final Algorithm algorithm;
    private final Indicators indicators;
    private final int budget;
    private final NondominatedPopulation referenceSet;
    private final Problem problem;

    public QueueData(Algorithm algorithm, Indicators indicators, int budget, NondominatedPopulation referenceSet,
            Problem problem) {
        this.algorithm = algorithm;
        this.indicators = indicators;
        this.budget = budget;
        this.referenceSet = referenceSet;
        this.problem = problem;
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

    public NondominatedPopulation getReferenceSet() {
        return this.referenceSet;
    }

    public Problem getProblem() {
        return this.problem;
    }
}
