package translate.complexity;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;

public interface ComplexityEvaluator {
    interface Clazz {
        double calculate(Node node);
    }

    interface Method {
        double calculate(MethodDeclaration method);
    }
}
