package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.complexity.MetricDefinition;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.result.ComplexityMetricResult;
import translate.structure.complexity.MethodLike;
import translate.translator.ComplexityTranslator;

@Getter
@AllArgsConstructor
public class WMCBaseEvaluator implements ComplexityEvaluator.Clazz {
    private final ComplexityEvaluator.Method methodEval;
    private final MetricDefinition definition;

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        double accumulator = 0;

        for (var method : clazz.getMethods()) {
            accumulator += evaluateMethod(translator, clazz, MethodLike.of(method));
        }

        for (var ctor : clazz.getConstructors()) {
            accumulator += evaluateMethod(translator, clazz, MethodLike.of(ctor));
        }

        return result(accumulator);
    }

    public double evaluateMethod(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodLike<?> method) {
        return methodEval.calculate(translator, clazz, method).getValue();
    }
}
