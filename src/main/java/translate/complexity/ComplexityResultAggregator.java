package translate.complexity;

import lombok.Getter;
import translate.complexity.result.BoundedResult;
import translate.complexity.result.ComplexityMetricResult;

import java.util.*;

@Getter
public class ComplexityResultAggregator {
    private final Map<String, BoundedResult> metrics = new HashMap<>();
    private final Set<String> codeSmells = new HashSet<>();

    private final BoundedResult[] rawMetricData;

    @Getter(lazy = true)
    private final boolean isValid = calculateValid();

    public ComplexityResultAggregator(BoundedResult[] results) {
        for (BoundedResult result : results) {
            if (result == null) continue;

            metrics.put(result.getDefinition().metric(), result);
        }

        this.rawMetricData = results;
    }

    private boolean calculateValid() {
        for (var result : metrics.values()) {
            if (!result.isValid()) return false;
        }

        return true;
    }

    public BoundedResult getMetric(String key) {
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

    public String metricsAi() {
        return metricsAi("\n");
    }

    public String metricsAi(String joining) {
        return String.join(joining, metricsAiList());
    }

    public String codeSmellsAi() {
        return String.join(", ", codeSmells);
    }
}
