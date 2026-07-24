package translate.complexity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.text.DecimalFormat;

@Getter
@ToString
public class ComplexityMetricResult {
    private static final DecimalFormat DF_3 = new DecimalFormat("0.###");
    private static final DecimalFormat DF_2 = new DecimalFormat("0.##");
    private static final DecimalFormat DF_1 = new DecimalFormat("0.#");
    private static final DecimalFormat DF_0 = new DecimalFormat("0");

    public enum Target {
        HIGHER, LOWER, NONE;

        public double normalize(double value, double min, double max) {
            if (this == NONE) return value;

            if (value < min || max < value) {
                return 0.0;
            }

            double normalized = (value - min) / (max - min);
            if (this == HIGHER) return normalized;
            else return 1.0 - normalized;
        }
    }

    private final String name;
    private final double max, min;
    private final double value;
    private final Target target;

    public ComplexityMetricResult(String name, double max, double min, double value, Target target) {
        this.name = name;
        this.max = max;
        this.min = min;
        this.value = value;
        this.target = target;
    }

    public ComplexityMetricResult(String name, double max, double min, double value) {
        this(name, max, min, value, Target.LOWER);
    }

    public boolean isValid() {
        return this.min < this.value && this.value < this.max;
    }

    public boolean isThresholdValid() {
        return target != Target.NONE && max < Double.MAX_VALUE / 2 && min > Double.MAX_VALUE / -2;
    }

    public double normalize() {
        return target.normalize(value, min, max);
    }

    public String toHtml(boolean color) {
        if (color) {
            return "<html><div style='white-space: nowrap;'>" + this.getName() + " = " + this.getValueWithColor() + "</div></html>";
        } else {
            return "<html><div style='white-space: nowrap;'>" + this.getName() + " = " + this.formattedValue() + "</div></html>";
        }
    }

    public String getValueWithColor() {
        if (this.isValid()) {
            return "<font color='green'> " + this.formattedValue() + "</font>";
        } else {
            return "<font color='red'> " + this.formattedValue() + "</font>";
        }
    }

    public String toAi() {
        return name + " range between: " + min + " & " + max + " value: " + formattedValue() + "\n";
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

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(name, max, min, value, target);
    }

    @Getter
    @Accessors(fluent = true)
    @ToString
    @AllArgsConstructor
    public static class Builder {
        private String name;
        private double max;
        private double min;
        private double value;
        private Target target = Target.LOWER;

        Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder min(double min) {
            this.min = min;
            return this;
        }

        public Builder value(double value) {
            this.value = value;
            return this;
        }

        public Builder target(Target target) {
            this.target = target;
            return this;
        }

        public ComplexityMetricResult build() {
            return new ComplexityMetricResult(this.name, this.max, this.min, this.value, this.target);
        }

        @Override
        public Builder clone() {
            return new Builder(name, max, min, value, target);
        }
    }
}
