package translate.complexity;

import org.tomlj.TomlTable;
import util.DoubleFormatter;

import java.util.Locale;

public record MetricDefinition(String metric, double min, double max, MetricTarget target) {

    public static MetricDefinition config(TomlTable table) {
        try {
            String metric = table.getString("metric");
            double min = Math.nextDown(table.getDouble("min"));
            double max = Math.nextUp(table.getDouble("max"));
            MetricTarget target = MetricTarget.valueOf(table.getString("target", () -> "LOWER").toUpperCase(Locale.ROOT));

            return new MetricDefinition(metric, min, max, target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static MetricDefinition dummy(String s) {
        return new MetricDefinition(s, 0, 0, MetricTarget.NONE);
    }

    public String printableThreshold() {
        return metric + " [" + DoubleFormatter.format(min) + ", " + DoubleFormatter.format(max) + "]";
    }
}
