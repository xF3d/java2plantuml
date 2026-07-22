package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.ResolverUtils;
import translate.component.formatter.MemberFormatter;

import java.util.Collection;

public class DITEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(Collection<TypeDeclaration<?>> list, Node node, MemberFormatter formatter) {
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("DIT")
                .max(4)
                .min(-1);

        if (node instanceof RecordDeclaration || node instanceof EnumDeclaration) {
            return builder.value(1).build();
        }

        if (node instanceof ClassOrInterfaceDeclaration coi && coi.isInterface()) {
            return builder.value(1).build();
        }


        int depth = 0;
        try {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) node;
            ResolvedClassDeclaration type = (ResolvedClassDeclaration) ResolverUtils.resolve(decl, ResolvedReferenceTypeDeclaration.class);

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
    }

    @Override
    public String getName() {
        return "DIT";
    }
}
