package translate.smells.clazz;

import translate.smells.CodeSmellEvaluator;
import translate.structure.complexity.ComplexityClass;
import translate.translator.ComplexityTranslator;

public class GodClassEvaluator implements CodeSmellEvaluator.Clazz {
    @Override
    public boolean calculate(ComplexityTranslator translator, ComplexityClass clazz) {
        return !clazz.evalMetric("LCOM1").isValid() &&
                !clazz.evalMetric("NOF").isValid() &&
                !clazz.evalMetric("NOM").isValid() &&
                !clazz.evalMetric("WMC-Cyclomatic").isValid();
    }

    @Override
    public String getType() {
        return "God Class";
    }
}
