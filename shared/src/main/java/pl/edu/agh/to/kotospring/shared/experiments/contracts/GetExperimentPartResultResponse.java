package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;

import java.util.List;
import java.util.Map;

public record GetExperimentPartResultResponse(
        List<AlgorithmResult> result,
        Map<String, Double> indicatorsValues
) {
}
