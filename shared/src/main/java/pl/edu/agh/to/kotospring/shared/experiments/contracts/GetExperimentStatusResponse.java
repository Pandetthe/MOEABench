package pl.edu.agh.to.kotospring.shared.experiments.contracts;

public record GetExperimentStatusResponse(
        ExperimentStatus status,
        String errorMessage
) {
}
