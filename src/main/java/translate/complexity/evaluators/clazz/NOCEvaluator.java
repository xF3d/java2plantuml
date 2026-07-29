package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.structure.complexity.ComplexityClass;
import translate.translator.ComplexityTranslator;

import java.util.ArrayList;

public class NOCEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        //TODO: should interface be counted?

        if (clazz instanceof RecordDeclaration || clazz instanceof EnumDeclaration) {
            return result(0);
        }

        CompilationUnit cu = clazz.findCompilationUnit().get();
        var formatter = translator.getFormatter();
        String fullPackageName = formatter.fullPackageName(cu, clazz);

        final int[] amount = {0};
        translator.getClassResultMap().values()
                .stream()
                .map(ComplexityClass::getNode)
                .forEach((entry) -> {
            ArrayList<ClassOrInterfaceType> typeList = new ArrayList<>();
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


        return result(amount[0]);
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("NOC");
    }
}
