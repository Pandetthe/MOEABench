package pl.edu.agh.to.kotospring.shared.experiments;

import java.util.Map;

public record AlgorithmResult(
        Map<String, Object> variables,
        Map<String, Double> objectives,
        Map<String, Double> constraints
) {
}
