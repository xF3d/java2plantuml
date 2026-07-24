package translate.smells;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
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
        boolean calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz);
    }

    interface Method extends CodeSmellEvaluator {
        boolean calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method);
    }
}
