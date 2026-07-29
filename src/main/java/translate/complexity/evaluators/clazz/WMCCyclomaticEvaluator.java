package translate.complexity.evaluators.clazz;

import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.MetricTarget;
import translate.complexity.evaluators.method.CyclomaticEvaluator;

public class WMCCyclomaticEvaluator extends WMCBaseEvaluator {
    public WMCCyclomaticEvaluator() {
        super(new CyclomaticEvaluator(), MetricDefinitionRegistry.get("WMC-Cyclomatic"));
    }
}
