package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.HashMap;
import java.util.List;

public record GetExperimentResultResponse(
        List<HashMap<String, Object>> result,
        HashMap<String, Double> indicatorsValues
) {
}
