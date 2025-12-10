package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.HashSet;
import java.util.Properties;

public record GetExperimentResponse(
        int id,
        String problem,
        Integer problemParameter,
        String algorithm,
        Properties algorithmParameters,
        HashSet<String> indicators,
        int budget
) {
}