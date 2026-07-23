package translate.complexity.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

@AllArgsConstructor
public class FanOutEvaluator implements ComplexityEvaluator.Clazz {
    private final CBOEvaluator cboEvaluator;

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        int fanOut = cboEvaluator.getProjectFanOut(translator, clazz).size();
        return new ComplexityMetricResult("FanOut", Integer.MAX_VALUE, 0, fanOut);
    }

    @Override
    public String getName() {
        return "FanOut";
    }
}
