package translate.complexity.result;

import translate.complexity.MetricDefinition;
import translate.complexity.MetricTarget;
import util.DoubleFormatter;

public interface BoundedResult {

    double getValue();

    MetricDefinition getDefinition();

    boolean isValid();

    double normalize();

    default String toHtml(boolean color) {
        var definition = getDefinition();
        if (color) {
            return "<html><div style='white-space: nowrap;'>" + definition.metric() + " = " + this.getValueWithColor() + "</div></html>";
        } else {
            return "<html><div style='white-space: nowrap;'>" + definition.metric() + " = " + this.formattedValue() + "</div></html>";
        }
    }

    default String getValueWithColor() {
        if (getDefinition().target() == MetricTarget.NONE) return this.formattedValue();

        if (this.isValid()) {
            return "<font color='green'> " + this.formattedValue() + "</font>";
        } else {
            return "<font color='red'> " + this.formattedValue() + "</font>";
        }
    }

    String toAi();

    default String formattedValue() {
        return DoubleFormatter.format(getValue());
    }
}
