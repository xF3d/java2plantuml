package translate.smells.clazz;

import translate.smells.CodeSmellEvaluator;
import translate.structure.complexity.ComplexityClass;
import translate.translator.ComplexityTranslator;

public class DataClassEvaluator implements CodeSmellEvaluator.Clazz {
    @Override
    public boolean calculate(ComplexityTranslator translator, ComplexityClass clazz) {
        double noc = clazz.evalMetricValue("NOC");
        double dit = clazz.evalMetricValue("DIT");
        double nof = clazz.evalMetricValue("NOF");

        return noc <= 1 && dit <= 2 && nof > 8;
    }

    @Override
    public String getType() {
        return "Data Class";
    }
}
