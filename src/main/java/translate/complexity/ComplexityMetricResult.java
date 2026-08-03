package translate.complexity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;

import java.text.DecimalFormat;

@ToString
@AllArgsConstructor
public class ComplexityMetricResult {
    private static final DecimalFormat DF_3 = new DecimalFormat("0.###");
    private static final DecimalFormat DF_2 = new DecimalFormat("0.##");
    private static final DecimalFormat DF_1 = new DecimalFormat("0.#");
    private static final DecimalFormat DF_0 = new DecimalFormat("0");

    @Delegate
    private final MetricDefinition definition;
    @Getter
    private final double value;


    public boolean isValid() {
        return this.min() < this.value && this.value < this.max();
    }

    public boolean isThresholdValid() {
        return target() != MetricTarget.NONE && max() < Double.MAX_VALUE / 2 && min() > Double.MAX_VALUE / -2;
    }

    public double normalize() {
        return target().normalize(value, min(), max());
    }

    public String toHtml(boolean color) {
        if (color) {
            return "<html><div style='white-space: nowrap;'>" + this.metric() + " = " + this.getValueWithColor() + "</div></html>";
        } else {
            return "<html><div style='white-space: nowrap;'>" + this.metric() + " = " + this.formattedValue() + "</div></html>";
        }
    }

    public String getValueWithColor() {
        if (target() == MetricTarget.NONE) return this.formattedValue();

        if (this.isValid()) {
            return "<font color='green'> " + this.formattedValue() + "</font>";
        } else {
            return "<font color='red'> " + this.formattedValue() + "</font>";
        }
    }

    public String toAi() {
        if (isThresholdValid()) {
            return metric() + " range between: " + min() + " & " + max() + " value: " + formattedValue();
        } else {
            return metric() + " value: " + formattedValue();
        }
    }

    public String formattedValue() {
        if (value == 0) {
            return "0";
        }

        double abs = Math.abs(value);

        if (abs < 1) {
            return DF_3.format(value);
        } else if (abs < 10) {
            return DF_2.format(value);
        } else if (abs < 100) {
            return DF_1.format(value);
        } else {
            return DF_0.format(value);
        }
    }
}
