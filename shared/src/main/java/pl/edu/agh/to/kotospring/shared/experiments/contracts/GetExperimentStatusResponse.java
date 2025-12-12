package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

public record GetExperimentStatusResponse(
        ExperimentStatus status
) {
}
