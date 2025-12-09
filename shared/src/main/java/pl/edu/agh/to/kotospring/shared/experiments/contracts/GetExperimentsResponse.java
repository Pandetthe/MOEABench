package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.Properties;

public record GetExperimentsResponse(
        int id,
        String problemName,
        Integer problemParameter,
        String algorithmName,
        Properties algorithmParameters,
        int budget
) {
}
