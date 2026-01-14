package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;

public record GetExperimentRunStatusResponse(
        ExperimentRunStatus status
) {
}
