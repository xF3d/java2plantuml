package translate.complexity.clazz;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

@AllArgsConstructor
public class FanInEvaluator implements ComplexityEvaluator.Clazz {
    private final CBOEvaluator cboEvaluator;

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        int fanIn = cboEvaluator.getProjectFanIn(translator, clazz).size();
        return new ComplexityMetricResult("FanIn", Integer.MAX_VALUE, 0, fanIn, ComplexityMetricResult.Target.HIGHER);
    }

    @Override
    public String getName() {
        return "FanIn";
    }
}
