package translate.smells.method;

import translate.smells.CodeSmellEvaluator;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.ComplexityTranslator;

public class LongMethodEvaluator implements CodeSmellEvaluator.Method {
    @Override
    public boolean calculate(ComplexityTranslator translator, ComplexityClass clazz, ComplexityMethod method) {
        return !method.evalMetric("Cyclomatic").isValid() &&
                !method.evalMetric("NBD").isValid() &&
                method.evalMetricValue("LOC") > 31;
    }

    @Override
    public String getType() {
        return "Long Method";
    }
}
