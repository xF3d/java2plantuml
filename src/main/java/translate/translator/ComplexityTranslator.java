package translate.translator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import file.Config;
import lombok.Getter;
import translate.complexity.*;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.evaluators.LOCEvaluator;
import translate.complexity.evaluators.clazz.*;
import translate.complexity.evaluators.method.CyclomaticEvaluator;
import translate.complexity.evaluators.method.NBDEvaluator;
import translate.complexity.evaluators.system.NumberOfClassesEvaluator;
import translate.complexity.evaluators.system.NumberOfTestsEvaluator;
import translate.smells.CodeSmellEvaluator;
import translate.smells.clazz.DataClassEvaluator;
import translate.smells.clazz.GodClassEvaluator;
import translate.smells.method.LongMethodEvaluator;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.structure.complexity.MethodLike;
import translate.translator.exceptions.InvalidSelectedClassException;
import translate.translator.exceptions.InvalidSelectedConstructorException;
import translate.translator.exceptions.InvalidSelectedMethodException;

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
            new NumberOfClassesEvaluator(),
            new NumberOfTestsEvaluator()
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

    @Getter(lazy = true)
    private final Map<String, Map<String, ComplexityMethod>> problematicMethods = evaluateProblematicMethods();

    @Getter(lazy = true)
    private final Map<String, ComplexityClass> problematicClasses = evaluateProblematicClasses();

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
                MethodLike<?> methodLike = MethodLike.of(method);

                var cmethod = new ComplexityMethod(methodLike, CompletableFuture.supplyAsync(() -> {
                    ComplexityMetricResult[] result = new ComplexityMetricResult[methodEvaluators.length];

                    for (int i = 0; i < methodEvaluators.length; i++) {
                        result[i] = methodEvaluators[i].calculate(this, classNode, methodLike);
                    }

                    return new ComplexityResultAggregator(result);
                }, executor));

                classResultMap.get(classFqn).addMethod(cmethod);
                methodResultMap.get(classFqn).put(methodEntry.getKey(), cmethod);
            }
        }

        // calculate constructor stuff
        for (var classMethodEntry : constructorsMap.entrySet()) {
            String classFqn = classMethodEntry.getKey();
            for (var ctorEntry : classMethodEntry.getValue().entrySet()) {
                methodResultMap.putIfAbsent(classFqn, new HashMap<>());

                TypeDeclaration<?> classNode = classMap.get(classFqn);
                ConstructorDeclaration ctor = ctorEntry.getValue();
                MethodLike<?> methodLike = MethodLike.of(ctor);

                var cmethod = new ComplexityMethod(methodLike, CompletableFuture.supplyAsync(() -> {
                    ComplexityMetricResult[] result = new ComplexityMetricResult[methodEvaluators.length];

                    for (int i = 0; i < methodEvaluators.length; i++) {
                        result[i] = methodEvaluators[i].calculate(this, classNode, methodLike);
                    }

                    return new ComplexityResultAggregator(result);
                }, executor));

                classResultMap.get(classFqn).addMethod(cmethod);
                methodResultMap.get(classFqn).put(ctorEntry.getKey(), cmethod);
            }
        }
        // </editor-fold>

        this.systemResult.whenComplete((r, e) -> computeSmells(executor));
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

    private Map<String, ComplexityClass> evaluateProblematicClasses() {
        var filter = Config.getInstance().getConfig().getPackageFilter();
        Map<String, ComplexityClass> output = new HashMap<>();
        for (var entry : classResultMap.entrySet()) {
            String fullPackageName = entry.getKey();
            if (!filter.isValid(fullPackageName)) continue;

            var clazz = entry.getValue();
            var results = clazz.evalResult();

            if (!results.isValid()) {
                output.put(fullPackageName, clazz);
            }
        }

        return output;
    }

    private Map<String, Map<String, ComplexityMethod>> evaluateProblematicMethods() {
        var filter = Config.getInstance().getConfig().getPackageFilter();
        Map<String, Map<String, ComplexityMethod>> output = new HashMap<>();
        for (var methodEntry : methodResultMap.entrySet()) {
            String classFqn = methodEntry.getKey();
            HashMap<String, ComplexityMethod> methodInvalidMap = new HashMap<>();

            if (!filter.isValid(classFqn)) continue;

            for (var classEntry : methodEntry.getValue().entrySet()) {
                String signature = classEntry.getKey();
                var method = classEntry.getValue();
                var results = method.evalResult();

                if (method.isMethod() && !results.isValid()) {
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
            return getClass(fullClassName).getComplexityResult().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ComplexityResultAggregator evaluateMethod(String fullClassName, String method) {
        try {
            return getMethod(fullClassName, method).getComplexityResult().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ComplexityClass getClass(String fullClassName) {
        var result = classResultMap.get(fullClassName);
        if (result == null) {
            throw new InvalidSelectedClassException(fullClassName);
        }

        return result;
    }

    public ComplexityMethod getMethod(String fullClassName, String method) throws InvalidSelectedClassException, InvalidSelectedMethodException {
        var mthMap = methodResultMap.get(fullClassName);
        if (mthMap == null) {
            throw new InvalidSelectedClassException(fullClassName);
        }

        var result = mthMap.get(method);
        if (result == null) {
            throw new InvalidSelectedMethodException(fullClassName, method);
        }

        return result;
    }

    @Override
    public TypeDeclaration<?> getTypeDeclaration(String fullClassName) throws InvalidSelectedClassException {
        try {
            return getClass(fullClassName).getNode();
        } catch (Exception e) {
            throw new InvalidSelectedClassException(fullClassName);
        }
    }

    @Override
    public MethodDeclaration getMethodDeclaration(String fullClassName, String method) throws InvalidSelectedClassException, InvalidSelectedMethodException {
        try {
            return getMethod(fullClassName, method).getMethod().raw().asMethodDeclaration();
        } catch (Exception e) {
            throw new InvalidSelectedMethodException(fullClassName, method);
        }
    }

    @Override
    public ConstructorDeclaration getConstructorDeclaration(String fullClassName, String method) throws InvalidSelectedClassException, InvalidSelectedConstructorException {
        try {
            return getMethod(fullClassName, method).getMethod().raw().asConstructorDeclaration();
        } catch (Exception e) {
            throw new InvalidSelectedConstructorException(fullClassName, method);
        }
    }


    public String[] getClassesArray() {
        return classResultMap.keySet().toArray(String[]::new);
    }

    public String[] getMethodsArray(String className) {
        return methodResultMap.get(className).keySet().toArray(String[]::new);
    }
}
