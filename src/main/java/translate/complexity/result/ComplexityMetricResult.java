package translate.complexity.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricTarget;
import util.DoubleFormatter;

@ToString
@AllArgsConstructor
public class ComplexityMetricResult implements BoundedResult {

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

    @Override
    public String toAi() {
        var definition = getDefinition();
        if (definition.isThresholdValid()) {
            return definition.metric() + " range between: " + definition.formattedMin() + " & " + definition.formattedMax() + " value: " + formattedValue();
        } else {
            return definition.metric() + " value: " + formattedValue();
        }
    }
}
