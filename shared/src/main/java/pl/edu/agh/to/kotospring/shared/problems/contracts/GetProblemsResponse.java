package pl.edu.agh.to.kotospring.shared.problems.contracts;

public record GetProblemsResponse(
        String name,
        boolean requiresParameter
) {
}
