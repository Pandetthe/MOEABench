package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

public record GetExperimentPartStatusResponse(
        ExperimentPartStatus status,
        String errorMessage
) {
}
