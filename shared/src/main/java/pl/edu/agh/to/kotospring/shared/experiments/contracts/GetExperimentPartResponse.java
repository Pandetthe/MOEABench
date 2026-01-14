package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.Map;

public record GetExperimentPartResponse(
        String algorithm,
        Map<String, String> parameters,
        String problem,
        Set<String> indicators,
        int budget,
        ExperimentPartStatus status,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {

}
