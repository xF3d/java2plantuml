package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.result.BoundedResult;
import translate.translator.ComplexityTranslator;

/**
 * Number of Methods
 */
public class NOMEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public BoundedResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        return result(clazz.getMethods().size());
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("NOM");
    }
}
