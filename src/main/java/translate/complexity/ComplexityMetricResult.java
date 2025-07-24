package translate.complexity;

import lombok.Data;

@Data
public class ComplexityMetricResult {
    private String name;
    private int max, min;
    private int value;
}
