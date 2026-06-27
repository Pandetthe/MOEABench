package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

public record GetExperimentAggregateData(
        String algorithm,
        Map<String, String> parameters,
        String problem,
        @JsonDeserialize(contentAs = GetExperimentAggregateDataIndicator.class)
        Map<String, GetExperimentAggregateDataIndicator> indicators,
        int budget) {
}