package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;

public record GetExperimentRunResponseData(
        long id,
        String algorithm,
        String problem,
        int budget,
        ExperimentPartStatus status,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
