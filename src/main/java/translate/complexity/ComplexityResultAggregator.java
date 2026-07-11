package translate.complexity;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ComplexityResultAggregator implements Iterable<ComplexityMetricResult> {
    public static final ComplexityResultAggregator EMPTY = new ComplexityResultAggregator(new ComplexityMetricResult[0]);

    @Getter
    private final Map<String, ComplexityMetricResult> aggregator = new HashMap<>();

    @Getter
    private final ComplexityMetricResult[] rawData;

    @Getter(lazy = true)
    private final boolean isValid = calculateValid();

    public ComplexityResultAggregator(ComplexityMetricResult[] results) {
        for (var result : results) {
            aggregator.put(result.getName(), result);
        }

        this.rawData = results;
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

    @Override
    public Iterator<ComplexityMetricResult> iterator() {
        return Arrays.stream(rawData).iterator();
    }
}
