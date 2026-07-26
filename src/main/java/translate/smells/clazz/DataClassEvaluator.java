package translate.smells.clazz;

import translate.smells.CodeSmellEvaluator;
import translate.structure.complexity.ComplexityClass;
import translate.translator.ComplexityTranslator;

public class DataClassEvaluator implements CodeSmellEvaluator.Clazz {
    @Override
    public boolean calculate(ComplexityTranslator translator, ComplexityClass clazz) {
        return false;
    }

    @Override
    public String getType() {
        return "Data Class";
    }
}
