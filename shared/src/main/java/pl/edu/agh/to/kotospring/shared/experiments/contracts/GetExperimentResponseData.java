package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

public record GetExperimentResponseData(
        long id,
        ExperimentPartStatus status,
        String errorMessage,
        String algorithm,
        String problem,
        int budget,
        Map<String, Object> algorithmParameters,
        Set<String> indicators,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
