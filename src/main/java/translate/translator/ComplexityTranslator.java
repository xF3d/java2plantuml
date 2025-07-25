package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import translate.ClassDiagramConfig;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.clazz.DITEvaluator;
import translate.complexity.clazz.NOCEvaluator;
import translate.complexity.clazz.WMCBaseEvaluator;
import translate.component.MemberFormatter;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class ComplexityTranslator implements Translator {
    public static ClassDiagramConfig config = new ClassDiagramConfig.DefaultDirector().construct();
    private Boolean error = false;

    private final Map<String, Node> classMap = new HashMap<>();
    private final Map<String, Map<String, MethodDeclaration>> methodMap = new HashMap<>();

    private static final ComplexityEvaluator.Clazz[] evaluators = {
            new DITEvaluator(),
            new NOCEvaluator(),
            new WMCBaseEvaluator("WMC/unity", 20,  -1, (a, b) -> ComplexityMetricResult.builder().value(1).build())
    };

    @Override
    public void addNode(Node node) {
        String fullName = MemberFormatter.fullPackageName(node);
        classMap.put(fullName, node);

        //TODO: this doesn't include inherited methods
        if (node instanceof NodeWithMembers<?> nwm) {
            Map<String, MethodDeclaration> methods = methodMap.get(fullName);
            if (methods == null) {
                methods = new HashMap<>();
            }

            for (MethodDeclaration method : nwm.getMethods()) {
                methods.put(method.getNameAsString(), method);
            }

            methodMap.put(fullName, methods);
        }
    }

    @Override
    public void setError(Boolean b) {
        error = b;
    }

    public ComplexityMetricResult[] evaluateClass(String fullClassName) {
        ComplexityMetricResult[] result = new ComplexityMetricResult[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            result[i] = evaluators[i].calculate(classMap.values(), classMap.get(fullClassName));
        }

        return result;
    }

    @Override
    public ClassDiagramConfig getConfig() {
        return config;
    }

    public JList<String> getClassJList() {
        return new JList<>(classMap.keySet().toArray(String[]::new));
    }

    public JList<String> getMethodsJList(String className) {
        return new JList<>(methodMap.get(className).keySet().toArray(String[]::new));
    }
}
