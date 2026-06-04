package translate.complexity;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ComplexityAggregator {
    private final Map<String, ComplexityMetricResult> aggregator = new HashMap<>();

    @Getter(lazy = true)
    private final boolean isValid = calculateValid();

    public ComplexityAggregator(ComplexityMetricResult[] results) {
        for (var result : results) {
            aggregator.put(result.getName(), result);
        }
    }

    private boolean calculateValid() {
        for (var result : aggregator.values()) {
            if (!result.isValid()) return false;
        }

        return true;
    }

    public ComplexityMetricResult get(String key) {
        return aggregator.get(key);
    }
}
