package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.complexity.MetricDefinition;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.MetricTarget;
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
            accumulator += evaluateMethod(translator, clazz, method);
        }

        return result(accumulator);
    }

    public double evaluateMethod(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method) {
        return methodEval.calculate(translator, clazz, method).getValue();
    }
}
