package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.Map;

public record GetExperimentAggregateData(
        String algorithm,
        Map<String, String> parameters,
        String problem,
        Map<String, GetExperimentAggregateDataIndicator> indicators,
        int budget) {
}