package translate.complexity.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

/**
 * Number of Fields
 */
public class NOFEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("NOF")
                .max(9)
                .min(-1);

        return builder.value(clazz.getFields().size()).build();
    }

    @Override
    public String getName() {
        return "NOF";
    }
}
