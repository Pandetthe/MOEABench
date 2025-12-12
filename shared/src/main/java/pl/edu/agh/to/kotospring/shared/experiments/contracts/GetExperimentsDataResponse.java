package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.time.OffsetDateTime;

public record GetExperimentsDataResponse(
        long id,
        ExperimentStatus status,
        OffsetDateTime queuedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}