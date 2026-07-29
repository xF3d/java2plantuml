package translate.complexity.clazz;

import translate.complexity.MetricTarget;
import translate.complexity.method.CyclomaticEvaluator;

public class WMCCyclomaticEvaluator extends WMCBaseEvaluator {
    public WMCCyclomaticEvaluator() {
        super(new CyclomaticEvaluator(), "WMC-Cyclomatic", 35, -1, MetricTarget.LOWER);
    }
}
