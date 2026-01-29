package pl.edu.agh.to.kotospring.shared.experiments.contracts;

public record ExperimentGroupRunRequest(
        Long experimentId,
        Long runNo
) {
}
