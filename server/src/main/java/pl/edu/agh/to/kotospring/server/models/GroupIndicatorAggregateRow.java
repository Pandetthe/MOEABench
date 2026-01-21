package pl.edu.agh.to.kotospring.server.models;

public record GroupIndicatorAggregateRow(
        String algorithm,
        String problem,
        String indicator,
        Double minValue,
        Double maxValue,
        Double meanValue,
        Double medianValue,
        Double iqrValue,
        Double stddevValue
) {
    public static GroupIndicatorAggregateRow fromNativeRow(Object[] row) {
        return new GroupIndicatorAggregateRow(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                toDouble(row[3]),
                toDouble(row[4]),
                toDouble(row[5]),
                toDouble(row[6]),
                toDouble(row[7]),
                toDouble(row[8])
        );
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
