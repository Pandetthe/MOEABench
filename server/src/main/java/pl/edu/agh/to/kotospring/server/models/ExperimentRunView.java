package pl.edu.agh.to.kotospring.server.models;

import pl.edu.agh.to.kotospring.server.entities.ExperimentPartExecution;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record ExperimentRunView(
        ExperimentRunStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<ExperimentPartExecution> parts
) {}
