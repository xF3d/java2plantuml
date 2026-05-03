package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.clazz.DITEvaluator;
import translate.complexity.clazz.LCOMNEvaluator;
import translate.complexity.clazz.NOCEvaluator;
import translate.complexity.clazz.RFCEvaluator;
import translate.complexity.clazz.WMCBaseEvaluator;
import translate.complexity.method.CyclomaticEvaluator;
import translate.component.MemberFormatter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexityTranslator implements Translator {
    private Boolean error = false;

    // full package name --> node
    private final Map<String, Node> classMap = new HashMap<>();

    // full package name --> method Name --> method
    private final Map<String, Map<String, MethodDeclaration>> methodMap = new HashMap<>();

    private static final ComplexityEvaluator.Clazz[] classEvaluators = {
            new DITEvaluator(),
            new NOCEvaluator(),
            new WMCBaseEvaluator("WMC/unity", 20,  -1, (a, b) -> ComplexityMetricResult.builder().value(1).build()),
            //new CBOEvaluator(),
            new RFCEvaluator(),
            new LCOMNEvaluator()
    };

    private static final ComplexityEvaluator.Method[] methodEvaluators = {
            new CyclomaticEvaluator()
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

    public List<String> getProblematicClassesUml() {
        List<String> output = new ArrayList<>();
        for (var fullPackageName : classMap.keySet()) {
            ComplexityMetricResult[] results = evaluateClass(fullPackageName);
            for (ComplexityMetricResult metricResult : results) {
                if (!metricResult.isValid()) {
                    if (classMap.get(fullPackageName) instanceof RecordDeclaration) {
                        output.add("record " + fullPackageName + " #Red\n");
                    } else {
                        output.add("class " + fullPackageName + " #Red\n");
                    }
                    break;
                }
            }
        }

        return output;
    }

    public ComplexityMetricResult[] evaluateClass(String fullClassName) {
        ComplexityMetricResult[] result = new ComplexityMetricResult[classEvaluators.length];
        Node classNode = classMap.get(fullClassName);
        if (classNode == null) {
            throw new RuntimeException("Not found: " + fullClassName);
        }

        for (int i = 0; i < classEvaluators.length; i++) {
            result[i] = classEvaluators[i].calculate(classMap.values(), classNode);
        }

        return result;
    }

    public ComplexityMetricResult[] evaluateMethod(String fullClassName, String method) {
        ComplexityMetricResult[] result = new ComplexityMetricResult[methodEvaluators.length];
        Node classNode = classMap.get(fullClassName);
        MethodDeclaration methodDeclaration = methodMap.get(fullClassName).get(method);
        for (int i = 0; i < methodEvaluators.length; i++) {
            result[i] = methodEvaluators[i].calculate(classNode, methodDeclaration);
        }

        return result;
    }

    public JList<String> getClassJList() {
        return new JList<>(classMap.keySet().toArray(String[]::new));
    }

    public JList<String> getMethodsJList(String className) {
        return new JList<>(methodMap.get(className).keySet().toArray(String[]::new));
    }
}
