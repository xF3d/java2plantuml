package translate.complexity;

import lombok.Getter;

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

            this.classMetrics.put(result.metric(), result);
        }

        for (ComplexityMetricResult result : systemMetrics) {
            if (result == null) continue;

            this.systemMetrics.put(result.metric(), result);
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
}
