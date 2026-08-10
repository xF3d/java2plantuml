package translate.translator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.Getter;
import translate.complexity.*;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.evaluators.LOCEvaluator;
import translate.complexity.evaluators.clazz.*;
import translate.complexity.evaluators.method.CyclomaticEvaluator;
import translate.complexity.evaluators.method.NBDEvaluator;
import translate.complexity.evaluators.system.NumberOfClassesEvaluator;
import translate.component.formatter.MemberFormatter;
import translate.smells.CodeSmellEvaluator;
import translate.smells.clazz.DataClassEvaluator;
import translate.smells.clazz.GodClassEvaluator;
import translate.smells.method.LongMethodEvaluator;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.structure.pkg.PackageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;

@Getter
public class ComplexityTranslator extends SimpleTranslator {

    private CompletableFuture<SystemResultAggregator> systemResult;
    private final Set<CompilationUnit> allCompilationUnits = new HashSet<>();

    private final Map<String, ComplexityClass> classResultMap = new ConcurrentHashMap<>();

    private final Map<String, Map<String, ComplexityMethod>> methodResultMap = new ConcurrentHashMap<>();

    private final ComplexityEvaluator.System[] systemEvaluators = {
            new LOCEvaluator(),
            new NumberOfClassesEvaluator()
    };
    
    private final ComplexityEvaluator.Clazz[] classEvaluators;

    private final ComplexityEvaluator.Method[] methodEvaluators = {
            new CyclomaticEvaluator(),
            new NBDEvaluator(),
            new LOCEvaluator()
    };

    private final CodeSmellEvaluator.Clazz[] classSmellEvaluators = {
            new DataClassEvaluator(),
            new GodClassEvaluator()
    };

    private final CodeSmellEvaluator.Method[] methodSmellEvaluators = {
            new LongMethodEvaluator()
    };

    public ComplexityTranslator() {
        var cbo = new CBOEvaluator();
        this.classEvaluators = new ComplexityEvaluator.Clazz[] {
                new DITEvaluator(),
                new NOCEvaluator(),
                new RFCEvaluator(),
                new LCOMStarEvaluator(),
                cbo,
                new FanInEvaluator(cbo),
                new FanOutEvaluator(cbo),
                new LOCEvaluator(),
                new NOFEvaluator(),
                new NOMEvaluator(),
                new WMCCyclomaticEvaluator()
        };
    }

    @Override
    public void handleCompilationUnit(CompilationUnit unit) {
        allCompilationUnits.add(unit);
    }

    @Override
    public void translateFiles(List<File> files) throws FileNotFoundException {
        super.translateFiles(files);

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        computeEverything(executor);
    }

    @Override
    public boolean hasExecuted() {
        return executed;
    }

    private void computeEverything(ExecutorService executor) {
        // <editor-fold desc="Metrics">
        // calculate class stuff
        for (var classEntry : classMap.entrySet()) {
            classResultMap.put(classEntry.getKey(), new ComplexityClass(classEntry.getValue(), CompletableFuture.supplyAsync(() -> {
                ComplexityMetricResult[] resultArray = new ComplexityMetricResult[classEvaluators.length];
                for (int i = 0; i < classEvaluators.length; i++) {
                    resultArray[i] = classEvaluators[i].calculate(this, classEntry.getValue());
                }

                return new ComplexityResultAggregator(resultArray);
            }, executor)));
        }


        var allClasses = classMap.values();
        var classResultFutures = classResultMap.values().stream().map(ComplexityClass::getComplexityResult).toList();

        // system metrics
        var systemResultsOnly = CompletableFuture.supplyAsync(() -> {
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
                if (classResults.isEmpty()) return new SystemResultAggregator(new ComplexityMetricResult[0], systemResultsOnly.join().getRawMetricData());

                ComplexityMetricResult[] classResultMetrics = new ComplexityMetricResult[classEvaluators.length];

                // get classes evaluation
                for (int i = 0; i < classEvaluators.length; i++) {
                    var ev = classEvaluators[i];
                    if (!ev.usefulForSystem()) continue;

                    MetricDefinition definition = ev.getDefinition();
                    String name = definition.metric();
                    double averageValue = 0.0;
                    double size = classResults.size();
                    for (var classResult : classResults) {
                        var m = classResult.getMetric(name);
                        averageValue += m.getValue() / size;
                    }

                    classResultMetrics[i] = new ComplexityMetricResult(definition, averageValue);
                }

                ComplexityMetricResult[] systemResultsOnlyMetrics;
                try {
                    systemResultsOnlyMetrics = systemResultsOnly.get().getRawMetricData();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                return new SystemResultAggregator(classResultMetrics, systemResultsOnlyMetrics);
            }, executor);

        // calculate method stuff
        for (var classMethodEntry : methodMap.entrySet()) {
            String classFqn = classMethodEntry.getKey();
            for (var methodEntry : classMethodEntry.getValue().entrySet()) {
                methodResultMap.putIfAbsent(classFqn, new HashMap<>());

                TypeDeclaration<?> classNode = classMap.get(classFqn);
                MethodDeclaration method = methodEntry.getValue();

                methodResultMap.get(classFqn).put(methodEntry.getKey(), new ComplexityMethod(method, CompletableFuture.supplyAsync(() -> {
                    ComplexityMetricResult[] result = new ComplexityMetricResult[methodEvaluators.length];

                    for (int i = 0; i < methodEvaluators.length; i++) {
                        result[i] = methodEvaluators[i].calculate(this, classNode, method);
                    }

                    return new ComplexityResultAggregator(result);
                }, executor)));
            }
        }
        // </editor-fold>

        this.systemResult.whenComplete((r, e) -> {
            classMap.clear();
            methodMap.clear();
            computeSmells(executor);
        });
    }

    private void computeSmells(ExecutorService executor) {
        ArrayList<CompletableFuture<?>> allCodeSmells = new ArrayList<>();

        for (var classEntry : classResultMap.entrySet()) {
            ComplexityClass complexityClass = classEntry.getValue();

            allCodeSmells.add(CompletableFuture.runAsync(() -> {
                for (CodeSmellEvaluator.Clazz classSmellEvaluator : classSmellEvaluators) {
                    boolean result = classSmellEvaluator.calculate(ComplexityTranslator.this, complexityClass);
                    if (result) {
                        complexityClass.getComplexityResult().join().addSmell(classSmellEvaluator.getType());
                    }
                }
            },executor));

            String fqn = classEntry.getKey();
            for (var methodEntry : methodResultMap.get(fqn).values()) {
                var complexityResult = methodEntry.getComplexityResult();

                allCodeSmells.add(CompletableFuture.runAsync(() -> {
                    for (CodeSmellEvaluator.Method methodSmellEvaluator : methodSmellEvaluators) {
                        boolean result = methodSmellEvaluator.calculate(ComplexityTranslator.this, complexityClass, methodEntry);
                        if (result) {
                            complexityResult.join().addSmell(methodSmellEvaluator.getType());
                        }
                    }

                }, executor));
            }
        }

        CompletableFuture.allOf(allCodeSmells.toArray(CompletableFuture[]::new)).whenComplete( (a, e) -> {
            executor.shutdownNow();
            e.printStackTrace();
        });
    }

    public Map<String, ComplexityClass> getProblematicClasses() {
        Map<String, ComplexityClass> output = new HashMap<>();
        for (var entry : classResultMap.entrySet()) {
            String fullPackageName = entry.getKey();
            var clazz = entry.getValue();
            var results = clazz.evalResult();

            if (!results.isValid()) {
                output.put(fullPackageName, clazz);
            }
        }

        return output;
    }

    public Map<String, Map<String, ComplexityMethod>> getProblematicMethods() {
        Map<String, Map<String, ComplexityMethod>> output = new HashMap<>();
        for (var methodEntry : methodResultMap.entrySet()) {
            String classFqn = methodEntry.getKey();
            HashMap<String, ComplexityMethod> methodInvalidMap = new HashMap<>();

            for (var classEntry : methodEntry.getValue().entrySet()) {
                String signature = classEntry.getKey();
                var method = classEntry.getValue();
                var results = method.evalResult();

                if (!results.isValid()) {
                    methodInvalidMap.put(signature, method);
                }
            }

            output.put(classFqn, methodInvalidMap);
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

    public SystemResultAggregator evaluateSystem() {
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
            return getMethod(fullClassName, method).getComplexityResult().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public ComplexityMethod getMethod(String fullClassName, String method) {
        var mthMap = methodResultMap.get(fullClassName);
        if (mthMap == null) {
            throw new IllegalArgumentException("Invalid class: " + fullClassName);
        }

        var result = mthMap.get(method);
        if (result == null) {
            throw new IllegalArgumentException("Invalid method: " + fullClassName + "#" + method);
        }

        return result;
    }

    public String[] getClassesArray() {
        return classResultMap.keySet().toArray(String[]::new);
    }

    public String[] getMethodsArray(String className) {
        return methodResultMap.get(className).keySet().toArray(String[]::new);
    }
}
