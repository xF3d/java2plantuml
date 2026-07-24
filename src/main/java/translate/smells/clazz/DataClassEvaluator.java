package translate.smells.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import translate.smells.CodeSmellEvaluator;
import translate.translator.ComplexityTranslator;

public class DataClassEvaluator implements CodeSmellEvaluator.Clazz {
    @Override
    public boolean calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        return false;
    }

    @Override
    public String getType() {
        return "Data Class";
    }
}
