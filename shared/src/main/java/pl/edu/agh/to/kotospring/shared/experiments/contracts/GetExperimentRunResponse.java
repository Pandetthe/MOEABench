package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record GetExperimentRunResponse(
        ExperimentRunStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<GetExperimentRunResponseData> parts
) {
}
