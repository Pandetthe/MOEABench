package pl.edu.agh.to.kotospring.server.models;

public record IndicatorAggregateRow(
        long definitionId,
        String indicator,
        double minValue,
        double maxValue,
        double meanValue,
        double medianValue,
        double iqrValue,
        double stddevValue
) {
    public static IndicatorAggregateRow fromNativeRow(Object[] r) {
        long definitionId = ((Number) r[0]).longValue();
        String indicator = (String) r[1];

        double min = ((Number) r[2]).doubleValue();
        double max = ((Number) r[3]).doubleValue();
        double mean = ((Number) r[4]).doubleValue();
        double median = ((Number) r[5]).doubleValue();
        double iqr = ((Number) r[6]).doubleValue();
        double stddev = ((Number) r[7]).doubleValue();

        return new IndicatorAggregateRow(definitionId, indicator, min, max, mean, median, iqr, stddev);
    }
}
