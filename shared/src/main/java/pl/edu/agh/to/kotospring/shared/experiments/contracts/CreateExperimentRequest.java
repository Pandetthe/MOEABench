package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.List;

public record CreateExperimentRequest(
        List<CreateExperimentRequestData> parts,
        long runCount
) {}