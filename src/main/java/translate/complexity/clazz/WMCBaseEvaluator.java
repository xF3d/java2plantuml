package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public abstract class WMCBaseEvaluator implements ComplexityEvaluator.Clazz {

    private final ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder();

    public WMCBaseEvaluator(String name, double max, double min) {
        builder.name(name)
                .max(max)
                .min(min);
    }

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        var b = builder.clone();
        double accumulator = 0;

        for (var method : clazz.getMethods()) {
            accumulator += evaluateMethod(translator, clazz, method).getValue();
        }

        return b.value(accumulator).build();
    }

    public abstract ComplexityMetricResult evaluateMethod(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method);

    @Override
    public String getName() {
        return builder.name();
    }
}
