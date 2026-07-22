package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;

import java.util.Collection;

public abstract class WMCBaseEvaluator implements ComplexityEvaluator.Clazz {

    private final ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder();

    public WMCBaseEvaluator(String name, double max, double min) {
        builder.name(name)
                .max(max)
                .min(min);
    }

    @Override
    public ComplexityMetricResult calculate(Collection<TypeDeclaration<?>> allClazz, Node clazz, MemberFormatter formatter) {
        var b = builder.clone();
        double accumulator = 0;

        if (clazz instanceof NodeWithMembers<?> nwm) {
            for (var method : nwm.getMethods()) {
                accumulator += evaluateMethod(clazz, method, formatter).getValue();
            }
        }

        return b.value(accumulator).build();
    }

    public abstract ComplexityMetricResult evaluateMethod(Node clazz, MethodDeclaration method, MemberFormatter formatter);

    @Override
    public String getName() {
        return builder.name();
    }
}
