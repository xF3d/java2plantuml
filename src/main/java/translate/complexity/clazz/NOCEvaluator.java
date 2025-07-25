package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.MemberFormatter;

import java.util.Collection;

public class NOCEvaluator implements ComplexityEvaluator.Clazz {
    private static final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
            .name("NOC")
            .max(Double.MAX_VALUE)
            .min(-1);

    @Override
    public ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz) {
        if (clazz instanceof RecordDeclaration || clazz instanceof EnumDeclaration) {
            return builder.value(0).build();
        }

        String fullPackageName = MemberFormatter.fullSimpleName(clazz);
        System.out.println(fullPackageName);

        int amount = 0;
        for (var entry : allClazz) {
            NodeList<ClassOrInterfaceType> typeList = new NodeList<>();
            if (entry instanceof NodeWithExtends<?> ext) {
                typeList.addAll(ext.getExtendedTypes());
            }

            if (entry instanceof NodeWithImplements<?> impl) {
                typeList.addAll(impl.getImplementedTypes());
            }

            for (var type : typeList) {
                String name = MemberFormatter.fullSimpleName(type);
                if (name.equals(fullPackageName)) {
                    amount++;
                    break;
                }
            }
        }


        return builder.value(amount).build();
    }
}
