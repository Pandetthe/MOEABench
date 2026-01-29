package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.List;

public record GetExperimentRunsResponse(
        List<GetExperimentRunsResponseData> content,
        PageMetadata metadata) {
}
