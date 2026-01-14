package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import java.time.OffsetDateTime;

public record GetExperimentRunsResponseData(
        long experimentId,
        long runNo,
        ExperimentRunStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}