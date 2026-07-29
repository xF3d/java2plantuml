package translate.complexity.evaluators.method;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

/**
 * Nested Blocks Depth
 */
public class NBDEvaluator implements ComplexityEvaluator.Method {
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method) {
        var builder = ComplexityMetricResult.builder()
                .name("NBD")
                .max(4)
                .min(-1);

        var bodyOpt = method.getBody();
        if (bodyOpt.isEmpty()) {
            return builder.value(0).build();
        }

        var value = scanDepth(bodyOpt.get(), -1);
        return builder.value(value).build();
    }

    @Override
    public String getName() {
        return "NBD";
    }

    private static int scanDepth(Node node, int depth) {
        int current = depth;

        if (node instanceof BlockStmt) {
            current++;
        }

        int max = current;

        for (Node child : node.getChildNodes()) {
            max = Math.max(max, scanDepth(child, current));
        }

        return max;
    }
}
