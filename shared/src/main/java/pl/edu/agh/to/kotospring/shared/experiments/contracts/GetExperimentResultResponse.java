package pl.edu.agh.to.kotospring.shared.experiments.contracts;

public record GetExperimentResultResponse(
        String[] variables,
        double[] objectives,
        double[] constraints
) {
}
