package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import lombok.Getter;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;
import translate.structure.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Getter
public class ComplexityTranslator implements Translator {
    private final PackageManager packageManager = new PackageManager();

    private Boolean error = false;

    private final MemberFormatter formatter = new MemberFormatter(packageManager);

    // full package name --> node
    private final Map<String, Node> classMap = new HashMap<>();

    // full package name --> method Signature --> method
    private final Map<String, Map<String, MethodDeclaration>> methodMap = new HashMap<>();

    @Override
    public void addNode(Node node) {
        // note, this is under teh assumption that every node passed here is a typeDefinition
        String fullName = formatter.fullPackageName(node.findCompilationUnit().get(), node);
        classMap.put(fullName, node);

        //TODO: this doesn't include inherited methods, are they really necessary tho?
        if (node instanceof NodeWithMembers<?> nwm) {
            Map<String, MethodDeclaration> methods = methodMap.get(fullName);
            if (methods == null) {
                methods = new HashMap<>();
            }

            for (MethodDeclaration method : nwm.getMethods()) {
                methods.put(method.getSignature().asString(), method);
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

    public ComplexityMetricResult[] evaluateSystem() {
        ComplexityMetricResult[] result = new ComplexityMetricResult[
                    ComplexityEvaluator.Clazz.EVALUATORS.length + ComplexityEvaluator.System.EVALUATORS.length
                ];

        if (classMap.isEmpty()) {
            return new ComplexityMetricResult[0];
        }

        var allClasses = classMap.values();

        // Evaluate system
        int j;
        for (j = 0; j < ComplexityEvaluator.System.EVALUATORS.length; j++) {
            result[j] = ComplexityEvaluator.System.EVALUATORS[j].calculate(this, allClasses, formatter);
        }

        // Evaluate Classes
        for (int i = 0; i < ComplexityEvaluator.Clazz.EVALUATORS.length; i++) {
            ArrayList<ComplexityMetricResult> values = new ArrayList<>();

            for (var classNode : classMap.values()) {
                values.add(ComplexityEvaluator.Clazz.EVALUATORS[i].calculate(allClasses, classNode, formatter));
            }

            double average = values.stream().mapToDouble(ComplexityMetricResult::getValue).average().getAsDouble();
            result[i + j] = values.get(0).toBuilder().value(average).build();
        }

        return result;
    }

    public ComplexityMetricResult[] evaluateClass(String fullClassName) {
        Node classNode = classMap.get(fullClassName);
        if (classNode == null) {
            throw new RuntimeException("Not found: " + fullClassName);
        }

        CompletableFuture<ComplexityMetricResult>[] tasks = new CompletableFuture[ComplexityEvaluator.Clazz.EVALUATORS.length];
        for (int i = 0; i < ComplexityEvaluator.Clazz.EVALUATORS.length; i++) {
            int finalI = i;
            tasks[i] = CompletableFuture.supplyAsync( () -> ComplexityEvaluator.Clazz.EVALUATORS[finalI].calculate(classMap.values(), classNode, formatter), ComplexityEvaluator.Clazz.EXECUTOR);
        }

        var completed = CompletableFuture.allOf(tasks);
        completed.join();

        ComplexityMetricResult[] result = new ComplexityMetricResult[ComplexityEvaluator.Clazz.EVALUATORS.length];
        for (int i = 0; i < ComplexityEvaluator.Clazz.EVALUATORS.length; i++) {
            result[i] = tasks[i].join();
        }

        return result;
    }

    public ComplexityMetricResult[] evaluateMethod(String fullClassName, String method) {
        ComplexityMetricResult[] result = new ComplexityMetricResult[ComplexityEvaluator.Method.EVALUATORS.length];
        Node classNode = classMap.get(fullClassName);
        MethodDeclaration methodDeclaration = methodMap.get(fullClassName).get(method);
        for (int i = 0; i < ComplexityEvaluator.Method.EVALUATORS.length; i++) {
            result[i] = ComplexityEvaluator.Method.EVALUATORS[i].calculate(classNode, methodDeclaration, formatter);
        }

        return result;
    }

    public String[] getClassJList() {
        return classMap.keySet().toArray(String[]::new);
    }

    public String[] getMethodsJList(String className) {
        return methodMap.get(className).keySet().toArray(String[]::new);
    }
}
