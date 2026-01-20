package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.Set;

public record GetExperimentGroupResponse(
        Long id,
        String name,
        Set<String> problems,
        Set<String> algorithms,
        Set<ExperimentGroupRunResponse> runs
) {
}
