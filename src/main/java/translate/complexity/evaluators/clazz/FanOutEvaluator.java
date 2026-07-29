package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

@AllArgsConstructor
public class FanOutEvaluator implements ComplexityEvaluator.Clazz {
    private final CBOEvaluator cboEvaluator;

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        int fanOut = cboEvaluator.getProjectFanOut(translator, clazz).size();
        return new ComplexityMetricResult("FanOut", 30, -1.0, fanOut);
    }

    @Override
    public String getName() {
        return "FanOut";
    }
}
