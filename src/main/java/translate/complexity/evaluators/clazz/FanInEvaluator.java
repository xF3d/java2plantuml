package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.result.BoundedResult;
import translate.translator.ComplexityTranslator;

@AllArgsConstructor
public class FanInEvaluator implements ComplexityEvaluator.Clazz {
    private final CBOEvaluator cboEvaluator;

    @Override
    public BoundedResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        int fanIn = cboEvaluator.getProjectFanIn(translator, clazz).size();
        return result(fanIn);
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("FanIn");
    }
}
