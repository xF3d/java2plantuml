package translate.complexity.clazz;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;

import java.util.Collection;

public class NOCEvaluator implements ComplexityEvaluator.Clazz {
    private static final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
            .name("NOC")
            .max(Double.MAX_VALUE)
            .min(-1);

    @Override
    public ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz, MemberFormatter formatter) {
        if (clazz instanceof RecordDeclaration || clazz instanceof EnumDeclaration) {
            return builder.value(0).build();
        }

        CompilationUnit cu = clazz.findCompilationUnit().get();
        String fullPackageName = formatter.fullPackageName(cu, clazz);

        int amount = 0;
        for (var entry : allClazz) {
            NodeList<ClassOrInterfaceType> typeList = new NodeList<>();
            if (entry instanceof NodeWithExtends<?> ext) {
                typeList.addAll(ext.getExtendedTypes());
            }

            if (entry instanceof NodeWithImplements<?> impl) {
                typeList.addAll(impl.getImplementedTypes());
            }

            for (ClassOrInterfaceType type : typeList) {
                String name = formatter.fullPackageName(cu, type);
                if (name.equals(fullPackageName)) {
                    amount++;
                    break;
                }
            }
        }


        return builder.value(amount).build();
    }
}
