package translate.translator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import lombok.Getter;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.ComplexityResultAggregator;
import translate.complexity.clazz.*;
import translate.complexity.method.CyclomaticEvaluator;
import translate.component.formatter.MemberFormatter;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.structure.pkg.PackageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


@Getter
public class ComplexityTranslator implements Translator {
    private final PackageManager packageManager = new PackageManager();
    private final MemberFormatter formatter = new MemberFormatter(packageManager);

    private CompletableFuture<ComplexityResultAggregator> systemResult; // todo: use this instead
    private final Set<CompilationUnit> allCompilationUnits = new HashSet<>();

    // full package name --> node
    private final Map<String, TypeDeclaration<?>> tempClassMap = new HashMap<>();
    private final Map<String, ComplexityClass> classResultMap = new HashMap<>();

    // full package name --> method Signature --> method
    private final Map<String, Map<String, MethodDeclaration>> tempMethodMap = new HashMap<>();
    private final Map<String, Map<String, ComplexityMethod>> methodResultMap = new HashMap<>();

    private final ComplexityEvaluator.System[] systemEvaluators = {
            
    };
    
    private final ComplexityEvaluator.Clazz[] classEvaluators;

    private final ComplexityEvaluator.Method[] methodEvaluators = {
            new CyclomaticEvaluator()
    };

    private Boolean error = false;

    public ComplexityTranslator() {
        var cbo = new CBOEvaluator();
        this.classEvaluators = new ComplexityEvaluator.Clazz[] {
                new DITEvaluator(),
                new NOCEvaluator(),
                new WMCBaseEvaluator("WMC/unity", 20, -1) {

                    @Override
                    public ComplexityMetricResult evaluateMethod(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodDeclaration method) {
                        return ComplexityMetricResult.builder().value(1).build();
                    }
                },
                new RFCEvaluator(),
                new LCOMNEvaluator(),
                cbo,
                new FanInEvaluator(cbo),
                new FanOutEvaluator(cbo)
        };
    }

    @Override
    public void addNode(Node node) {
        if (!(node instanceof TypeDeclaration<?> decl)) throw new IllegalArgumentException("Should never happen");

        String fullName = formatter.fullPackageName(node.findCompilationUnit().orElseThrow(), node);
        tempClassMap.put(fullName, decl);

        //TODO: this doesn't include inherited methods, are they really necessary tho?
        Map<String, MethodDeclaration> methods = tempMethodMap.get(fullName);
        if (methods == null) {
            methods = new HashMap<>();
        }

        for (MethodDeclaration method : decl.getMethods()) {
            methods.put(method.getSignature().asString(), method);
        }

        tempMethodMap.put(fullName, methods);
    }

    @Override
    public void handleCompilationUnit(CompilationUnit unit) {
        allCompilationUnits.add(unit);
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
        for (var classEntry : tempClassMap.entrySet()) {
            classResultMap.put(classEntry.getKey(), new ComplexityClass(classEntry.getValue(), CompletableFuture.supplyAsync(() -> {
                ComplexityMetricResult[] resultArray = new ComplexityMetricResult[classEvaluators.length];
                for (int i = 0; i < classEvaluators.length; i++) {
                    resultArray[i] = classEvaluators[i].calculate(this, classEntry.getValue());
                }

                return new ComplexityResultAggregator(resultArray);
            }, executor)));
        }


        var allClasses = tempClassMap.values();
        var classResultFutures = classResultMap.values().stream().map(ComplexityClass::getComplexityResult).toList();

        // system metrics
        var systemResults = CompletableFuture.supplyAsync(() -> {
            ComplexityMetricResult[] result = new ComplexityMetricResult[systemEvaluators.length];
            for (int i = 0; i < systemEvaluators.length; i++) {
                result[i] = systemEvaluators[i].calculate(this, allClasses);
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
                    classEvaluators.length + systemEvaluators.length
                ];

                int j;
                try {
                    var rawSystemResults = systemResults.get().getRawData();
                    for (j = 0; j < systemEvaluators.length; j++) {
                        result[j] = rawSystemResults[j];
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                // get classes evaluation
                // TODO: expose builder and handle context-less case
                for (int i = j; i < classEvaluators.length; i++) {
                    String name = classEvaluators[i].getName();
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
        for (var classMethodEntry : tempMethodMap.entrySet()) {
            String classFqn = classMethodEntry.getKey();
            for (var methodEntry : classMethodEntry.getValue().entrySet()) {
                methodResultMap.putIfAbsent(classFqn, new HashMap<>());
                methodResultMap.get(classFqn).put(methodEntry.getKey(), new ComplexityMethod(methodEntry.getValue(), CompletableFuture.supplyAsync(() -> {
                    ComplexityMetricResult[] result = new ComplexityMetricResult[methodEvaluators.length];
                    TypeDeclaration<?> classNode = tempClassMap.get(classFqn);
                    MethodDeclaration methodDeclaration = methodEntry.getValue();
                    for (int i = 0; i < methodEvaluators.length; i++) {
                        result[i] = methodEvaluators[i].calculate(this, classNode, methodDeclaration);
                    }

                    return new ComplexityResultAggregator(result);
                }, executor)));
            }
        }

        this.systemResult.whenComplete((r, e) -> {
            tempClassMap.clear();
            tempMethodMap.clear();
            executor.shutdown();
        });
    }

    @Override
    public void setError(Boolean b) {
        error = b;
    }

    public Map<String, ComplexityResultAggregator> getProblematicClasses() {
        Map<String, ComplexityResultAggregator> output = new HashMap<>();
        for (var fullPackageName : classResultMap.keySet()) {
            ComplexityResultAggregator results = evaluateClass(fullPackageName);
            if (!results.isValid()) {
                output.put(fullPackageName, results);
            }
        }

        return output;
    }

    public List<String> getProblematicClassesUml() {
        List<String> output = new ArrayList<>();
        for (var fullPackageName : classResultMap.keySet()) {
            ComplexityResultAggregator results = evaluateClass(fullPackageName);
            if (!results.isValid()) {
                if (classResultMap.get(fullPackageName).getNode() instanceof RecordDeclaration) {
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
            return classResultMap.get(fullClassName).getComplexityResult().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public ComplexityResultAggregator evaluateMethod(String fullClassName, String method) {
        try {
            return methodResultMap.get(fullClassName).get(method).getComplexityResult().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getClassesArray() {
        return classResultMap.keySet().toArray(String[]::new);
    }

    public String[] getMethodsArray(String className) {
        return methodResultMap.get(className).keySet().toArray(String[]::new);
    }
}
