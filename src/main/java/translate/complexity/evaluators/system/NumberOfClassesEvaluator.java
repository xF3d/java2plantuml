package translate.complexity.evaluators.system;

import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public class NumberOfClassesEvaluator implements ComplexityEvaluator.System {
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<TypeDeclaration<?>> allClazz) {
        return result(allClazz.size());
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("Number of Classes");
    }
}
