package translate.complexity.evaluators.system;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public class NumberOfTestsEvaluator implements ComplexityEvaluator.System {
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<TypeDeclaration<?>> allClazz) {
        int tests = 0;
        for (var clazz : allClazz) {
            for (var method : clazz.findAll(MethodDeclaration.class)) {
                if (isTestMethod(method)) {
                    tests++;
                }
            }
        }

        return result(tests);
    }

    public static boolean isTestMethod(MethodDeclaration method) {
        for (AnnotationExpr annotation : method.getAnnotations()) {
            if (annotation instanceof MarkerAnnotationExpr mae) {
                if (mae.getNameAsString().equals("Test")) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("Number of Tests");
    }
}
