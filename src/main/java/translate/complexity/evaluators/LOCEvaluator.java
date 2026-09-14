package translate.complexity.evaluators;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.result.BoundedResult;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.structure.complexity.MethodLike;
import translate.translator.ComplexityTranslator;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

public class LOCEvaluator implements ComplexityEvaluator.System, ComplexityEvaluator.Clazz, ComplexityEvaluator.Method {

    // system eval
    @Override
    public BoundedResult calculate(ComplexityTranslator translator, Collection<TypeDeclaration<?>> allClazz) {
        int lineTotal = 0;

        // CompilationUnit does not implement proper equals or hashCode
        HashMap<Path, CompilationUnit> unitHashMap = new HashMap<>();
        for (var clazz: allClazz) {
            CompilationUnit cu = clazz.findCompilationUnit().orElseThrow();
            Path path = cu.getStorage().orElseThrow().getPath();

            unitHashMap.put(path, cu);
        }

        for (CompilationUnit cu: unitHashMap.values()) {
            var rangeOpt = cu.getRange();
            if (rangeOpt.isEmpty()) continue;

            var range = rangeOpt.get();
            lineTotal += range.getLineCount();
        }

        return result(lineTotal);
    }

    // class eval
    @Override
    public BoundedResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        var rangeOpt = clazz.getRange();
        if (rangeOpt.isEmpty()) return result(0);

        var range = rangeOpt.get();

        return result(range.getLineCount());
    }

    // method eval
    @Override
    public BoundedResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodLike<?> method) {
        var rangeOpt = method.raw().getRange();
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
