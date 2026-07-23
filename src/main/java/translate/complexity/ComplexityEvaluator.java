package translate.complexity;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public interface ComplexityEvaluator {
    String getName();

    interface System extends ComplexityEvaluator {
        ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<TypeDeclaration<?>> allClazz);
    }

    interface Clazz extends ComplexityEvaluator {
        ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz);
    }

    interface Method extends ComplexityEvaluator {
        ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method);
    }
}
