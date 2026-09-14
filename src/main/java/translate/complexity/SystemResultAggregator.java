package translate.complexity;

import lombok.Getter;
import translate.complexity.result.ComplexityMetricResult;

import java.util.*;

@Getter
public class SystemResultAggregator {
    private final Map<String, ComplexityMetricResult> classMetrics = new HashMap<>();
    private final Map<String, ComplexityMetricResult> systemMetrics = new HashMap<>();

    @Getter(lazy = true)
    private final boolean isValid = calculateValid();

    public SystemResultAggregator(ComplexityMetricResult[] classMetrics, ComplexityMetricResult[] systemMetrics) {
        for (ComplexityMetricResult result : classMetrics) {
            if (result == null) continue;

            this.classMetrics.put(result.getDefinition().metric(), result);
        }

        for (ComplexityMetricResult result : systemMetrics) {
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

    public ArrayList<ComplexityMetricResult>[] displayList() {
        ArrayList<ComplexityMetricResult> chartResults = new ArrayList<>();
        ArrayList<ComplexityMetricResult> otherResults = new ArrayList<>();

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
}
