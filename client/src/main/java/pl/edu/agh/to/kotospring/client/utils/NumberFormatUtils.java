package pl.edu.agh.to.kotospring.client.utils;

import java.util.Locale;

public final class NumberFormatUtils {

    private NumberFormatUtils() {
    }

    public static String formatDoubleShort(Double value) {
        if (value == null)
            return "-";
        double number = value;
        double absoluteValue = Math.abs(number);
        if (absoluteValue != 0.0 && (absoluteValue < 1e-3 || absoluteValue >= 1e4)) {
            return String.format(Locale.ROOT, "%.3e", number).replace("e+0", "e+").replace("e-0", "e-");
        }
        String formattedValue  = String.format(Locale.ROOT, "%.4f", number);
        formattedValue = formattedValue.replaceAll("0+$", "").replaceAll("\\.$", "");
        return formattedValue.isEmpty() ? "0" : formattedValue ;
    }
    public static String formatAny(Object value) {
        if (value == null)
            return "-";
        if (value instanceof Double d)
            return formatDoubleShort(d);
        if (value instanceof Float f)
            return formatDoubleShort(f.doubleValue());
        if (value instanceof Number n)
            return formatDoubleShort(n.doubleValue());
        return String.valueOf(value);
    }

    public static String formatDouble(Double v) {
        return formatDoubleShort(v);
    }
}
