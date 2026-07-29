package translate.smells;

import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.ComplexityTranslator;


public interface CodeSmellEvaluator {
    String getType();

    interface Clazz extends CodeSmellEvaluator {
        boolean calculate(ComplexityTranslator translator, ComplexityClass clazz);
    }

    interface Method extends CodeSmellEvaluator {
        boolean calculate(ComplexityTranslator translator, ComplexityClass clazz, ComplexityMethod method);
    }
}
