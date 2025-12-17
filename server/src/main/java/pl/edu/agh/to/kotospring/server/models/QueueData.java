package pl.edu.agh.to.kotospring.server.models;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;

public record QueueData(
        Long experimentPartId,
        Algorithm algorithm,
        Indicators indicators,
        int budget) {
}
