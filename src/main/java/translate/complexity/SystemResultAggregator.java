package translate.complexity;

import lombok.Getter;
import translate.complexity.result.BoundedResult;
import translate.complexity.result.ComplexityMetricResult;

import java.util.*;

@Getter
public class SystemResultAggregator {
    private final Map<String, BoundedResult> classMetrics = new HashMap<>();
    private final Map<String, BoundedResult> systemMetrics = new HashMap<>();

    @Getter(lazy = true)
    private final boolean isValid = calculateValid();

    public SystemResultAggregator(BoundedResult[] classMetrics, BoundedResult[] systemMetrics) {
        for (BoundedResult result : classMetrics) {
            if (result == null) continue;

            this.classMetrics.put(result.getDefinition().metric(), result);
        }

        for (BoundedResult result : systemMetrics) {
            if (result == null) continue;

            this.systemMetrics.put(result.getDefinition().metric(), result);
        }
    }

    private boolean calculateValid() {
        for (var result : classMetrics.values()) {
            if (!result.isValid()) return false;
        }

        for (var result : systemMetrics.values()) {
            if (!result.isValid()) return false;
        }

        return true;
    }

    public ArrayList<BoundedResult>[] displayList() {
        ArrayList<BoundedResult> chartResults = new ArrayList<>();
        ArrayList<BoundedResult> otherResults = new ArrayList<>();

        for (var result : systemMetrics.values()) {
            if (result.getDefinition().isThresholdValid()) {
                chartResults.add(result);
            }

            otherResults.add(result);
        }

        for (var result : classMetrics.values()) {
            if (result.getDefinition().isThresholdValid()) {
                chartResults.add(result);
            }

            otherResults.add(result);
        }

        return new ArrayList[] {chartResults, otherResults};
    }

    public List<String> metricsAiList() {
        ArrayList<String> out = new ArrayList<>();

        for (var metric : classMetrics.values()) {
            out.add(metric.toAi());
        }

        for (var metric : systemMetrics.values()) {
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
}
