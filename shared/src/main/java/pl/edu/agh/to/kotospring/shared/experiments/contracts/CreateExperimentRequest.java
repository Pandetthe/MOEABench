package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.Properties;

public record CreateExperimentRequest(
        String problemName,
        Integer problemParameter,
        String algorithmName,
        Properties algorithmParameters,
        int budget
) {
}