package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;

public class DITEvaluator implements ComplexityEvaluator.Clazz {
    private static final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
            .name("DIT")
            .max(4)
            .min(-1);

    @Override
    public ComplexityMetricResult calculate(Node node) {
        return null;
    }
}
