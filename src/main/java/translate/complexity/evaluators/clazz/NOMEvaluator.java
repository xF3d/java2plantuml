package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

/**
 * Number of Methods
 */
public class NOMEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("NOM")
                .max(15)
                .min(-1);

        return builder.value(clazz.getMethods().size()).build();
    }

    @Override
    public String getName() {
        return "NOM";
    }
}
