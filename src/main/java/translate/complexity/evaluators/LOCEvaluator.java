package translate.complexity.evaluators;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.MetricTarget;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public class LOCEvaluator implements ComplexityEvaluator.System, ComplexityEvaluator.Clazz, ComplexityEvaluator.Method {

    // system eval
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<TypeDeclaration<?>> allClazz) {
        int lineTotal = 0;

        for (CompilationUnit cu: translator.getAllCompilationUnits()) {
            var rangeOpt = cu.getRange();
            if (rangeOpt.isEmpty()) continue;

            var range = rangeOpt.get();
            lineTotal += range.getLineCount();
        }

        return result(lineTotal);
    }

    // class eval
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        var rangeOpt = clazz.getRange();
        if (rangeOpt.isEmpty()) return result(0);

        var range = rangeOpt.get();

        return result(range.getLineCount());
    }

    // method eval
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method) {
        var rangeOpt = method.getRange();
        if (rangeOpt.isEmpty()) return result(0);

        var range = rangeOpt.get();

        return result(range.getLineCount());
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("LOC");
    }

    // just use custom impl
    @Override
    public boolean usefulForSystem() {
        return false;
    }
}
