package pl.edu.agh.to.kotospring.shared.algorithms.contracts;

import java.util.List;

public record GetAlgorithmsResponse(
        String name,
        List<String> parameters
) {
}
