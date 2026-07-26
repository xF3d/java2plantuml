package translate.smells.method;

import com.github.javaparser.ast.body.MethodDeclaration;
import translate.smells.CodeSmellEvaluator;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.ComplexityTranslator;

public class FeatureEnvyEvaluator implements CodeSmellEvaluator.Method {
    @Override
    public boolean calculate(ComplexityTranslator translator, ComplexityClass clazz, ComplexityMethod method) {
        return false;
    }

    @Override
    public String getType() {
        return "Feature Envy";
    }
}
