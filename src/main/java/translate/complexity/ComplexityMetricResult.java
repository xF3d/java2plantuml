package translate.complexity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ComplexityMetricResult {
    private String name;
    private double max, min;
    private double value;

    public boolean isValid() {
        return this.min < this.value && this.value < this.max;
    }
}
