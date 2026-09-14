package translate.complexity;

import org.tomlj.TomlTable;
import translate.complexity.result.BoundedResult;
import translate.complexity.result.ResultType;
import util.DoubleFormatter;

import java.util.Locale;

public record MetricDefinition(
        String metric,
        double min, double max, MetricTarget target,
        String repairStrategy,
        ResultType resultType
) {

    public static MetricDefinition config(TomlTable table) {
        try {
            String metric = table.getString("metric");
            double min = Math.nextDown(table.getDouble("min"));
            double max = Math.nextUp(table.getDouble("max"));
            MetricTarget target = MetricTarget.valueOf(table.getString("target", () -> "LOWER").toUpperCase(Locale.ROOT));
            String repairStrategy = table.getString("repair_strategy");
            ResultType resultType = ResultType.valueOf(table.getString("type", () -> "INT"));
            return new MetricDefinition(metric, min, max, target, repairStrategy, resultType);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static MetricDefinition dummy(String s) {
        return new MetricDefinition(s, 0, 0, MetricTarget.NONE, null, ResultType.INT);
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

    public boolean isThresholdValid() {
        return target != MetricTarget.NONE;
    }

    public BoundedResult result(double value) {
        return resultType.create(this, value);
    }
}
