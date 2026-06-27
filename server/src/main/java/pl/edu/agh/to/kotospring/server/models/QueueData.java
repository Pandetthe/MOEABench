package pl.edu.agh.to.kotospring.server.models;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;

public record QueueData(
        Long experimentPartId,
        Algorithm algorithm,
        Indicators indicators,
        int budget,
        NondominatedPopulation referenceSet,
        Problem problem
) {}
