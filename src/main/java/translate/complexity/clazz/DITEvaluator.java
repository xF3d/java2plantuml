package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.ComplexityUtils;

public class DITEvaluator implements ComplexityEvaluator.Clazz {
    private static final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
            .name("DIT")
            .max(4)
            .min(-1);

    @Override
    public ComplexityMetricResult calculate(Node node) {
        if (node instanceof RecordDeclaration || node instanceof EnumDeclaration) {
            return builder.value(1).build();
        }

        if (node instanceof ClassOrInterfaceDeclaration coi && coi.isInterface()) {
            return builder.value(1).build();
        }


        int depth = 0;
        try {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) node;
            ResolvedClassDeclaration type = (ResolvedClassDeclaration) ComplexityUtils.resolve(decl);

            while (type.getSuperClass().isPresent()) {
                depth++;
                ResolvedReferenceType whatever = type.getSuperClass().get();
                type = (ResolvedClassDeclaration) whatever.getTypeDeclaration().get();
            }

            return builder.value(depth).build();
        } catch (Throwable e) {
            // we reached a library class
            e.printStackTrace();
            return builder.value(depth + 1).build();
        }
    }
}
