package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.result.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

@AllArgsConstructor
public class FanOutEvaluator implements ComplexityEvaluator.Clazz {
    private final CBOEvaluator cboEvaluator;

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        int fanOut = cboEvaluator.getProjectFanOut(translator, clazz).size();
        return result(fanOut);
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("FanOut");
    }
}
