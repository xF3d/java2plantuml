package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;

import java.util.Collection;

public class WMCBaseEvaluator implements ComplexityEvaluator.Clazz {

    private final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder();
    private final ComplexityEvaluator.Method methodEvaluator;

    public WMCBaseEvaluator(String name, double max, double min, ComplexityEvaluator.Method methodEvaluator) {
        builder.name(name)
                .max(max)
                .min(min);
        this.methodEvaluator = methodEvaluator;
    }

    public WMCBaseEvaluator(String name, ComplexityEvaluator.Method methodEvaluator) {
        this(name, Double.MAX_VALUE, Double.MIN_VALUE, methodEvaluator);
    }

    @Override
    public ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz) {
        double accumulator = 0;

        if (clazz instanceof NodeWithMembers<?> nwm) {
            for (var method : nwm.getMethods()) {
                accumulator += methodEvaluator.calculate(clazz, method).getValue();
            }
        }

        return builder.value(accumulator).build();
    }
}
