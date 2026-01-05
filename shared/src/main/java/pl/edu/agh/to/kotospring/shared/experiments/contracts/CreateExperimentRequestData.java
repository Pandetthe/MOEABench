package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.Map;
import java.util.Set;

public record CreateExperimentRequestData(
        String problem,
        String algorithm,
        Map<String, Object> algorithmParameters,
        Set<String> indicators,
        int budget
) {
}