package translate.complexity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class ComplexityMetricResult {
    public enum Target {
        HIGHER, LOWER;

        public double normalize(double value, double min, double max) {
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
    private final Target target = Target.LOWER;

    public boolean isValid() {
        return this.min < this.value && this.value < this.max;
    }

    public double normalize() {
        return target.normalize(value, min, max);
    }
}
