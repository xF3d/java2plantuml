package translate.complexity;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public class LOCEvaluator implements ComplexityEvaluator.System, ComplexityEvaluator.Clazz, ComplexityEvaluator.Method {
    private static final ComplexityMetricResult.Builder BUILDER = ComplexityMetricResult.builder()
            .name("LOC")
            .max(Double.MAX_VALUE)
            .min(0.0)
            .value(0.0)
            .target(MetricTarget.NONE);

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

        return BUILDER.clone().value(lineTotal).build();
    }

    // class eval
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        var rangeOpt = clazz.getRange();
        if (rangeOpt.isEmpty()) return BUILDER.build();

        var range = rangeOpt.get();

        return BUILDER.clone().value(range.getLineCount()).build();
    }

    // method eval
    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method) {
        var rangeOpt = method.getRange();
        if (rangeOpt.isEmpty()) return BUILDER.build();

        var range = rangeOpt.get();

        return BUILDER.clone().value(range.getLineCount()).build();
    }

    @Override
    public String getName() {
        return "LOC";
    }

    // just use custom impl
    @Override
    public boolean usefulForSystem() {
        return false;
    }
}
