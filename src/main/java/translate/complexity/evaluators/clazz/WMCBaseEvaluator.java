package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.MetricTarget;
import translate.translator.ComplexityTranslator;

public class WMCBaseEvaluator implements ComplexityEvaluator.Clazz {
    private final ComplexityEvaluator.Method methodEval;
    private final ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder();

    public WMCBaseEvaluator(ComplexityEvaluator.Method methodEval, String name, double max, double min, MetricTarget target) {
        this.methodEval = methodEval;
        this.builder.name(name)
                .max(max)
                .min(min)
                .target(target);
    }

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        var b = builder.clone();
        double accumulator = 0;

        for (var method : clazz.getMethods()) {
            accumulator += evaluateMethod(translator, clazz, method);
        }

        return b.value(accumulator).build();
    }

    public double evaluateMethod(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method) {
        return methodEval.calculate(translator, clazz, method).getValue();
    }

    @Override
    public String getName() {
        return builder.name();
    }
}
