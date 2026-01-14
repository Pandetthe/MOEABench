package pl.edu.agh.to.kotospring.shared.experiments.contracts;

public record GetExperimentAggregateDataIndicator(
        double min,
        double max,
        double mean,
        double median,
        double iqr,
        double standardDeviation
) {
}
