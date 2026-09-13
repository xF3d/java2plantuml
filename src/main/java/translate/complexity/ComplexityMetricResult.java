package translate.complexity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;
import util.DoubleFormatter;

import java.text.DecimalFormat;

@ToString
@AllArgsConstructor
public class ComplexityMetricResult {

    @Getter
    private final MetricDefinition definition;
    @Getter
    private final double value;


    public boolean isValid() {
        return this.definition.min() < this.value && this.value < this.definition.max();
    }

    public double normalize() {
        return this.definition.target().normalize(value, definition.min(), definition.max());
    }

    public String toHtml(boolean color) {
        if (color) {
            return "<html><div style='white-space: nowrap;'>" + this.definition.metric() + " = " + this.getValueWithColor() + "</div></html>";
        } else {
            return "<html><div style='white-space: nowrap;'>" + this.definition.metric() + " = " + this.formattedValue() + "</div></html>";
        }
    }

    public String getValueWithColor() {
        if (definition.target() == MetricTarget.NONE) return this.formattedValue();

        if (this.isValid()) {
            return "<font color='green'> " + this.formattedValue() + "</font>";
        } else {
            return "<font color='red'> " + this.formattedValue() + "</font>";
        }
    }

    public String toAi() {
        if (definition.isThresholdValid()) {
            return definition.metric() + " range between: " + definition.formattedMin() + " & " + definition.formattedMax() + " value: " + formattedValue();
        } else {
            return definition.metric() + " value: " + formattedValue();
        }
    }

    public String formattedValue() {
        return DoubleFormatter.format(value);
    }
}
