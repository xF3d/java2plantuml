package translate.complexity;

import org.tomlj.TomlTable;
import util.DoubleFormatter;

import java.util.Locale;

public record MetricDefinition(String metric, double min, double max, MetricTarget target, String repairStrategy) {

    public static MetricDefinition config(TomlTable table) {
        try {
            String metric = table.getString("metric");
            double min = Math.nextDown(table.getDouble("min"));
            double max = Math.nextUp(table.getDouble("max"));
            MetricTarget target = MetricTarget.valueOf(table.getString("target", () -> "LOWER").toUpperCase(Locale.ROOT));
            String repairStrategy = table.getString("repair_strategy");
            return new MetricDefinition(metric, min, max, target, repairStrategy);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static MetricDefinition dummy(String s) {
        return new MetricDefinition(s, 0, 0, MetricTarget.NONE, null);
    }

    public String printableThreshold() {
        return metric + " [" + formattedMin() + ", " + formattedMax() + "]";
    }

    public String formattedMin() {
        return DoubleFormatter.format(min);
    }

    public String formattedMax() {
        return DoubleFormatter.format(max);
    }

    public String fullRepairStrategy() {
        return metric + " : " + repairStrategy;
    }
}
