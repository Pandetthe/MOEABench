package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.List;

public record GetExperimentResultResponseData(
        Long runNo,
        List<GetExperimentResultResponseDataPart> parts
) {
}
