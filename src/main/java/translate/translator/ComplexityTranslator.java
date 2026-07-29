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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO: complessità ciclomatica della classe = somma di quella dei metodi
 *
 * per il sistema = ciclomatica media delle classi
 * per ogni classe = ciclomatica somma dei metodi
 * (per ogni classe WMC-Ciclomatica) / n.classi
 */
@Getter
public class ComplexityTranslator implements Translator {
    private final PackageManager packageManager = new PackageManager();
    private final MemberFormatter formatter = new MemberFormatter(packageManager);

    private CompletableFuture<SystemResultAggregator> systemResult;
    private final Set<CompilationUnit> allCompilationUnits = new HashSet<>();

    // full package name --> node
    private final Map<String, TypeDeclaration<?>> tempClassMap = new HashMap<>();
    private final Map<String, ComplexityClass> classResultMap = new HashMap<>();

    // full package name --> method Signature --> method
    private final Map<String, Map<String, MethodDeclaration>> tempMethodMap = new HashMap<>();
    private final Map<String, Map<String, ComplexityMethod>> methodResultMap = new HashMap<>();

    private final ComplexityEvaluator.System[] systemEvaluators = {
            new LOCEvaluator()
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

    private Boolean error = false;

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

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        computeEverything(executor);
    }

    private void computeEverything(ExecutorService executor) {
        // <editor-fold desc="Metrics">
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
        for (var classMethodEntry : tempMethodMap.entrySet()) {
            String classFqn = classMethodEntry.getKey();
            for (var methodEntry : classMethodEntry.getValue().entrySet()) {
                methodResultMap.putIfAbsent(classFqn, new HashMap<>());

                TypeDeclaration<?> classNode = tempClassMap.get(classFqn);
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
            tempClassMap.clear();
            tempMethodMap.clear();
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

    @Override
    public void setError(Boolean b) {
        error = b;
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
