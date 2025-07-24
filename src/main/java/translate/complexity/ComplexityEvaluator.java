package translate.complexity;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;

public interface ComplexityEvaluator {
    interface Clazz {
        ComplexityMetricResult calculate(Node node);
    }

    interface Method {
        ComplexityMetricResult calculate(MethodDeclaration method);
    }
}
