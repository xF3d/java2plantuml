package translate.smells;

import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.ComplexityTranslator;

/**
 * todo implement
 * LONG_METHOD,
 *     DATA_CLASS,
 *     GOD_CLASS,
 *     FEATURE_ENVY
 */
public interface CodeSmellEvaluator {
    String getType();

    interface Clazz extends CodeSmellEvaluator {
        boolean calculate(ComplexityTranslator translator, ComplexityClass clazz);
    }

    interface Method extends CodeSmellEvaluator {
        boolean calculate(ComplexityTranslator translator, ComplexityClass clazz, ComplexityMethod method);
    }
}
