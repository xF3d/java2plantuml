package translate.complexity.evaluators;

import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.result.ComplexityMetricResult;
import translate.complexity.MetricDefinition;
import translate.structure.complexity.MethodLike;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public interface ComplexityEvaluator {
    MetricDefinition getDefinition();

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
        ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodLike<?> method);
    }

    default ComplexityMetricResult result(double value) {
        return new ComplexityMetricResult(getDefinition(), value);
    }
}
