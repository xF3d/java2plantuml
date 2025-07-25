package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.ComplexityUtils;

import java.util.Objects;

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

        try {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) node;
            ResolvedReferenceTypeDeclaration type = ComplexityUtils.resolve(decl);

            int depth = 0;
            while (type != null) {
                String qName = type.getQualifiedName();
                if (qName.equals("java.lang.Object")) break;
                if (type.getQualifiedName().isEmpty()) break;
                depth++;

                type = type.getAllAncestors()
                        .stream()
                        .map(ResolvedReferenceType::getTypeDeclaration)
                        .map((optional) -> optional.orElse(null))
                        .filter(Objects::nonNull)
                        .filter((typeDecl) -> !typeDecl.isInterface())
                        .findFirst()
                        .orElse(null);
            }

            return builder.value(depth).build();
        } catch (Exception e) {
            e.printStackTrace();
            return builder.value(1).build();
        }
    }
}
