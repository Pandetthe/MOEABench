package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public record GetExperimentResponse(
        ExperimentStatus status,
        OffsetDateTime queuedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<GetExperimentResponseData> parts
) {
}