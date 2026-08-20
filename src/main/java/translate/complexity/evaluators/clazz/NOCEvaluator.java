package translate.complexity.evaluators.clazz;

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
import translate.structure.pkg.PackageManager;
import translate.translator.ComplexityTranslator;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NOCEvaluator implements ComplexityEvaluator.Clazz {
    private volatile boolean initialized = false;
    private final Map<String, Integer> nocMap = new ConcurrentHashMap<>();

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        if (clazz instanceof RecordDeclaration || clazz instanceof EnumDeclaration) {
            return result(0);
        }

        init(translator);

        String fqn = PackageManager.resolveTypeDeclaration(clazz);
        return result(nocMap.getOrDefault(fqn, 0));
    }


    private void init(ComplexityTranslator translator) {

        if (initialized) return;

        synchronized (this) {
            if (initialized) return;

            var formatter = translator.getFormatter();

            translator.getClassMap().values()
                    .forEach((entry) -> {
                        ArrayList<ClassOrInterfaceType> typeList = new ArrayList<>();
                        if (entry instanceof NodeWithExtends<?> ext) {
                            typeList.addAll(ext.getExtendedTypes());
                        }

                        // TODO should interface be considered?
                        if (entry instanceof NodeWithImplements<?> impl) {
                            typeList.addAll(impl.getImplementedTypes());
                        }

                        for (ClassOrInterfaceType type : typeList) {
                            String name = formatter.fullPackageName(type);
                            nocMap.put(name, 1 + nocMap.getOrDefault(name, 0));
                        }
                    });


            initialized = true;
        }
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("NOC");
    }
}
