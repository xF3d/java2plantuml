package translate.smells.method;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.smells.CodeSmellEvaluator;
import translate.translator.ComplexityTranslator;

public class FeatureEnvyEvaluator implements CodeSmellEvaluator.Method {
    @Override
    public boolean calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method) {
        return false;
    }

    @Override
    public String getType() {
        return "Feature Envy";
    }
}
