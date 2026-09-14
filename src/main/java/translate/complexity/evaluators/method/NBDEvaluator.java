package translate.complexity.evaluators.method;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.result.BoundedResult;
import translate.structure.complexity.MethodLike;
import translate.translator.ComplexityTranslator;

/**
 * Nested Blocks Depth
 */
public class NBDEvaluator implements ComplexityEvaluator.Method {
    @Override
    public BoundedResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodLike<?> method) {
        var bodyOpt = method.body();
        if (bodyOpt.isEmpty()) {
            return result(0);
        }

        var value = scanDepth(bodyOpt.get(), -1);
        return result(value);
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

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("NBD");
    }
}
