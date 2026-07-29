package translate.complexity.evaluators;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.MetricDefinition;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public interface ComplexityEvaluator {
    String getName();

    //MetricDefinition getDefinition();

    interface System extends ComplexityEvaluator {
        ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<TypeDeclaration<?>> allClazz);
    }

    interface Clazz extends ComplexityEvaluator {
        ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz);

        /**
         * Whatever or not this metric result can be aggregated to get a system evaluation
         */
        default boolean usefulForSystem() {
            return true;
        }
    }

    interface Method extends ComplexityEvaluator {
        ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method);
    }
}
