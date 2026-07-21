package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import lombok.Getter;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.ComplexityResultAggregator;
import translate.component.formatter.MemberFormatter;
import translate.structure.PackageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


@Getter
public class ComplexityTranslator implements Translator {
    private final PackageManager packageManager = new PackageManager();
    private final MemberFormatter formatter = new MemberFormatter(packageManager);

    private CompletableFuture<ComplexityResultAggregator> systemResult; // todo: use this instead

    // full package name --> node
    private final Map<String, Node> classMap = new HashMap<>();
    private final Map<String, CompletableFuture<ComplexityResultAggregator>> classResultMap = new HashMap<>();

    // full package name --> method Signature --> method
    private final Map<String, Map<String, MethodDeclaration>> methodMap = new HashMap<>();
    private final Map<String, Map<String, CompletableFuture<ComplexityResultAggregator>>> methodResultMap = new HashMap<>();

    private Boolean error = false;

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
    public void translateFiles(List<File> files) throws FileNotFoundException {
        Translator.super.translateFiles(files);
        calculateMetrics();
    }

    private void calculateMetrics() {
        var threads = Runtime.getRuntime().availableProcessors();
        var executor = Executors.newFixedThreadPool(threads);

        // calculate class stuff
        for (var classEntry : classMap.entrySet()) {
            classResultMap.put(classEntry.getKey(), CompletableFuture.supplyAsync(() -> {
                ComplexityMetricResult[] resultArray = new ComplexityMetricResult[ComplexityEvaluator.Clazz.EVALUATORS.length];
                for (int i = 0; i < ComplexityEvaluator.Clazz.EVALUATORS.length; i++) {
                    resultArray[i] = ComplexityEvaluator.Clazz.EVALUATORS[i].calculate(classMap.values(), classEntry.getValue(), formatter);
                }

                return new ComplexityResultAggregator(resultArray);
            }, executor));
        }


        var allClasses = classMap.values();
        var classResultFutures = classResultMap.values();

        // system metrics
        var systemResults = CompletableFuture.supplyAsync(() -> {
            ComplexityMetricResult[] result = new ComplexityMetricResult[ComplexityEvaluator.System.EVALUATORS.length];
            for (int i = 0; i < ComplexityEvaluator.System.EVALUATORS.length; i++) {
                result[i] = ComplexityEvaluator.System.EVALUATORS[i].calculate(this, allClasses, formatter);
            }

            return new ComplexityResultAggregator(result);
        }, executor);

        // cumulative system metrics
        this.systemResult = CompletableFuture.allOf(classResultFutures.toArray(CompletableFuture[]::new))
            .thenApplyAsync(v ->
                classResultFutures.stream()
                    .map(CompletableFuture::join)
                    .toList(), executor
            ).thenApplyAsync(classResults -> {
                if (classResults.isEmpty()) return ComplexityResultAggregator.EMPTY;

                ComplexityMetricResult[] result = new ComplexityMetricResult[
                    ComplexityEvaluator.Clazz.EVALUATORS.length + ComplexityEvaluator.System.EVALUATORS.length
                ];

                int j;
                try {
                    var rawSystemResults = systemResults.get().getRawData();
                    for (j = 0; j < ComplexityEvaluator.System.EVALUATORS.length; j++) {
                        result[j] = rawSystemResults[j];
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                // get classes evaluation
                // TODO: expose builder and handle context-less case
                for (int i = 0; i < ComplexityEvaluator.Clazz.EVALUATORS.length; i++) {
                    String name = ComplexityEvaluator.Clazz.EVALUATORS[i].getName();
                    double average = 0.0;
                    ComplexityMetricResult.Builder builder = null;
                    for (var classResult : classResults) {
                        if (builder == null) builder = classResult.get(name).toBuilder();

                        average += classResult.get(name).getValue();
                    }

                    average /= classResults.size();

                    if (builder == null) throw new RuntimeException("impossible");
                    result[i + j] = builder.value(average).build();
                }

                return new ComplexityResultAggregator(result);
            }, executor);

        // calculate method stuff
        for (var classMethodEntry : methodMap.entrySet()) {
            String classFqn = classMethodEntry.getKey();
            for (var methodEntry : classMethodEntry.getValue().entrySet()) {
                methodResultMap.putIfAbsent(classFqn, new HashMap<>());
                methodResultMap.get(classFqn).put(methodEntry.getKey(), CompletableFuture.supplyAsync(() -> {
                    ComplexityMetricResult[] result = new ComplexityMetricResult[ComplexityEvaluator.Method.EVALUATORS.length];
                    Node classNode = classMap.get(classFqn);
                    MethodDeclaration methodDeclaration = methodEntry.getValue();
                    for (int i = 0; i < ComplexityEvaluator.Method.EVALUATORS.length; i++) {
                        result[i] = ComplexityEvaluator.Method.EVALUATORS[i].calculate(classNode, methodDeclaration, formatter);
                    }

                    return new ComplexityResultAggregator(result);
                }, executor));
            }
        }

        this.systemResult.whenComplete((r, e) -> executor.shutdown());
    }

    @Override
    public void setError(Boolean b) {
        error = b;
    }

    public Map<String, ComplexityResultAggregator> getProblematicClasses() {
        Map<String, ComplexityResultAggregator> output = new HashMap<>();
        for (var fullPackageName : classMap.keySet()) {
            ComplexityResultAggregator results = evaluateClass(fullPackageName);
            if (!results.isValid()) {
                output.put(fullPackageName, results);
            }
        }

        return output;
    }

    public List<String> getProblematicClassesUml() {
        List<String> output = new ArrayList<>();
        for (var fullPackageName : classMap.keySet()) {
            ComplexityResultAggregator results = evaluateClass(fullPackageName);
            if (!results.isValid()) {
                if (classMap.get(fullPackageName) instanceof RecordDeclaration) {
                    output.add("record " + fullPackageName + " #Red\n");
                } else {
                    output.add("class " + fullPackageName + " #Red\n");
                }
                break;
            }
        }

        return output;
    }

    public ComplexityResultAggregator evaluateSystem() {
        try {
            return systemResult.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public ComplexityResultAggregator evaluateClass(String fullClassName) {
        try {
            return classResultMap.get(fullClassName).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public ComplexityResultAggregator evaluateMethod(String fullClassName, String method) {
        try {
            return methodResultMap.get(fullClassName).get(method).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getClassesArray() {
        return classMap.keySet().toArray(String[]::new);
    }

    public String[] getMethodsArray(String className) {
        return methodMap.get(className).keySet().toArray(String[]::new);
    }
}
