package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.HashSet;
import java.util.Properties;

public record CreateExperimentRequest(
        String problem,
        String algorithm,
        Properties algorithmParameters,
        HashSet<String> indicators,
        int budget
) {
}