package translate.complexity;

import lombok.Getter;

import java.util.*;

@Getter
public class ComplexityResultAggregator {
    private final Map<String, ComplexityMetricResult> metrics = new HashMap<>();
    private final Set<String> codeSmells = new HashSet<>();

    private final ComplexityMetricResult[] rawMetricData;

    @Getter(lazy = true)
    private final boolean isValid = calculateValid();

    public ComplexityResultAggregator(ComplexityMetricResult[] results) {
        for (ComplexityMetricResult result : results) {
            if (result == null) continue;

            metrics.put(result.metric(), result);
        }

        this.rawMetricData = results;
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

    public void addSmell(String string) {
        codeSmells.add(string);
    }

    public List<String> metricsAiList() {
        ArrayList<String> out = new ArrayList<>();
        for (var metric : rawMetricData) {
            out.add(metric.toAi());
        }

        return out;
    }
}
