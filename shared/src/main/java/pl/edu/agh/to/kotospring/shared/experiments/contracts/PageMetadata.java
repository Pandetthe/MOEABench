package pl.edu.agh.to.kotospring.shared.experiments.contracts;

public record PageMetadata(
        long size,
        long totalElements,
        long totalPages,
        long number) {
}
