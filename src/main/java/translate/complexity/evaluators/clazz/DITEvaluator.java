package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.ResolverUtils;
import translate.translator.ComplexityTranslator;

public class DITEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("DIT")
                .max(4)
                .min(-1);

        if (clazz instanceof RecordDeclaration || clazz instanceof EnumDeclaration) {
            return builder.value(1).build();
        }

        if (clazz instanceof ClassOrInterfaceDeclaration coi) {
            if (coi.isInterface()) return builder.value(1).build();

            int depth = 0;
            try {
                ResolvedClassDeclaration type = ResolverUtils.resolve(coi, ResolvedClassDeclaration.class);

                while (type.getSuperClass().isPresent()) {
                    depth++;
                    ResolvedReferenceType whatever = type.getSuperClass().get();
                    type = (ResolvedClassDeclaration) whatever.getTypeDeclaration().get();
                }

                return builder.value(depth).build();
            } catch (Throwable e) {
                // we reached a library class
                return builder.value(depth + 1).build();
            }
        } else {
            // annotation
            return builder.value(1).build();
        }
    }

    @Override
    public String getName() {
        return "DIT";
    }
}
