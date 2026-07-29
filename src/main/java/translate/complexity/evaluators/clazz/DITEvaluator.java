package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.ResolverUtils;
import translate.translator.ComplexityTranslator;

public class DITEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {

        if (clazz instanceof RecordDeclaration || clazz instanceof EnumDeclaration) {
            return result(1);
        }

        if (clazz instanceof ClassOrInterfaceDeclaration coi) {
            if (coi.isInterface()) return result(1);

            int depth = 0;
            try {
                ResolvedClassDeclaration type = ResolverUtils.resolve(coi, ResolvedClassDeclaration.class);

                while (type.getSuperClass().isPresent()) {
                    depth++;
                    ResolvedReferenceType whatever = type.getSuperClass().get();
                    type = (ResolvedClassDeclaration) whatever.getTypeDeclaration().get();
                }

                return result(depth);
            } catch (Throwable e) {
                // we reached a library class
                return result(depth + 1);
            }
        } else {
            // annotation
            return result(1);
        }
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("DIT");
    }
}
