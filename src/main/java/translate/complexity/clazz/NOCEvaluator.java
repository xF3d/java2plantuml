package translate.complexity.clazz;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;
import translate.structure.complexity.ComplexityClass;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public class NOCEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        //TODO: should interface be counted?
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("NOC")
                .max(4)
                .min(-1);

        if (clazz instanceof RecordDeclaration || clazz instanceof EnumDeclaration) {
            return builder.value(0).build();
        }

        CompilationUnit cu = clazz.findCompilationUnit().get();
        var formatter = translator.getFormatter();
        String fullPackageName = formatter.fullPackageName(cu, clazz);

        final int[] amount = {0};
        translator.getClassResultMap().values()
                .stream()
                .map(ComplexityClass::getNode)
                .forEach((entry) -> {
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
                    amount[0]++;
                    break;
                }
            }
        });


        return builder.value(amount[0]).build();
    }

    @Override
    public String getName() {
        return "NOC";
    }
}
