package translate.complexity;

import lombok.Getter;

import java.util.*;

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

    public List<String> toAiList() {
        ArrayList<String> out = new ArrayList<>();
        for (var metric : this) {
            out.add(metric.toAi());
        }

        return out;
    }

    @Override
    public Iterator<ComplexityMetricResult> iterator() {
        return Arrays.stream(rawData).iterator();
    }
}
