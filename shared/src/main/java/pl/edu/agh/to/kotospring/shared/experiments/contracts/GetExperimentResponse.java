package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record GetExperimentResponse(
        ExperimentStatus status,
        OffsetDateTime queuedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        long runCount,
        List<GetExperimentResponseData> runs
) {
}