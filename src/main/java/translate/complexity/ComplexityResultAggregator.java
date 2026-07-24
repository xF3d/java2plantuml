package translate.complexity;

import lombok.Getter;

import java.util.*;

@Getter
public class ComplexityResultAggregator implements Iterable<ComplexityMetricResult> {
    public static final ComplexityResultAggregator EMPTY = new ComplexityResultAggregator(new ComplexityMetricResult[0]);

    private final Map<String, ComplexityMetricResult> metrics = new HashMap<>();
    private final Set<String> codeSmells = new HashSet<>();

    private final ComplexityMetricResult[] rawData;

    @Getter(lazy = true)
    private final boolean isValid = calculateValid();

    public ComplexityResultAggregator(ComplexityMetricResult[] results) {
        for (var result : results) {
            metrics.put(result.getName(), result);
        }

        this.rawData = results;
    }

    private boolean calculateValid() {
        for (var result : metrics.values()) {
            if (!result.isValid()) return false;
        }

        return true;
    }

    public ComplexityMetricResult getMetric(String key) {
        return metrics.get(key);
    }

    public List<String> toAiList() {
        ArrayList<String> out = new ArrayList<>();
        for (var metric : this) {
            out.add(metric.toAi());
        }

        return out;
    }

    public void addSmell(String string) {
        codeSmells.add(string);
    }

    @Override
    public Iterator<ComplexityMetricResult> iterator() {
        return Arrays.stream(rawData).iterator();
    }
}
