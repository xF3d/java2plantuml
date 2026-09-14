package translate.complexity.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import translate.complexity.MetricDefinition;

@ToString
@AllArgsConstructor
public class IntMetricResult implements BoundedResult {

    @Getter
    private final MetricDefinition definition;
    @Getter
    private final double value;

    public boolean isValid() {
        return this.definition.min() <= this.value && this.value <= this.definition.max();
    }

    public double normalize() {
        return this.definition.target().normalizeInt(value, (int) definition.min(), (int) definition.max());
    }
}
