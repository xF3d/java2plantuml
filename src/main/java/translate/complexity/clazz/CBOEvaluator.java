package translate.complexity.clazz;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.AllArgsConstructor;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.structure.complexity.ComplexityClass;
import translate.structure.pkg.PackageManager;
import translate.translator.ComplexityTranslator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts the number of other classes that a class is coupled to.
 * High CBO indicates strong dependencies, making the class harder to modify, test, and reuse. Lower coupling generally improves maintainability.
 */
public class CBOEvaluator implements ComplexityEvaluator.Clazz {
    //TODO soglie configurabili
    private volatile boolean initialized = false;
    private final Map<String, Set<String>> projectFanOutMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> projectFanInMap = new ConcurrentHashMap<>();

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("CBO")
                .max(14)
                .min(-1);

        init(translator);

        String className = PackageManager.resolveTypeDeclaration(clazz);

        Set<String> cboSet = new HashSet<>(projectFanOutMap.get(className));
        cboSet.addAll(projectFanInMap.get(className));
        int cbo = cboSet.size();

        // TODO: testing
        return builder.value(cbo).build();
    }

    private void init(ComplexityTranslator translator) {
        if (initialized) return;

        synchronized (this) {
            if (initialized) return;

            translator.getClassResultMap().values().stream()
                    .map(ComplexityClass::getNode)
                    .forEach(decl -> {
                        // Ottieni il nome qualificato completo della classe (es. com.azienda.MetricApp)
                        String className = PackageManager.resolveTypeDeclaration(decl);
                        Set<String> dependencies = new HashSet<>();

                        // Esegui il visitatore per raccogliere i tipi usati
                        decl.accept(new DependencyVisitor(translator.getPackageManager(), decl.findCompilationUnit().orElseThrow()), dependencies);

                        // Rimuovi la classe stessa dalle sue dipendenze (auto-coupling non conta)
                        dependencies.remove(className);

                        projectFanOutMap.put(className, dependencies);

                    });

            for (String className : projectFanOutMap.keySet()) {
                projectFanInMap.put(className, new HashSet<>());
            }

            // Calcola il Fan-In invertendo la mappa del Fan-Out
            for (Map.Entry<String, Set<String>> entry : projectFanOutMap.entrySet()) {
                String currentClass = entry.getKey();
                for (String targetClass : entry.getValue()) {
                    if (projectFanInMap.containsKey(targetClass)) {
                        projectFanInMap.get(targetClass).add(currentClass);
                    }
                }
            }


            initialized = true;
        }
    }

    @AllArgsConstructor
    private static class DependencyVisitor extends VoidVisitorAdapter<Set<String>> {
        private final PackageManager packageManager;
        private final CompilationUnit context;

        @Override
        public void visit(ClassOrInterfaceType n, Set<String> arg) {
            super.visit(n, arg);
            String name = n.getNameAsString();
            handleName(name, arg);
        }

        @Override
        public void visit(ObjectCreationExpr n, Set<String> arg) {
            super.visit(n, arg);
            String name = n.getTypeAsString();
            handleName(name, arg);
        }

        private void handleName(String name, Set<String> arg) {
            String result = packageManager.resolveClass(context, name);

            if (result == null) {
                arg.add("external " + name);
            } else {
                arg.add(result);
            }
        }
    }

    public Set<String> getProjectFanOut(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        init(translator);
        String className = PackageManager.resolveTypeDeclaration(clazz);
        return Collections.unmodifiableSet(projectFanOutMap.get(className));
    }

    public Set<String> getProjectFanIn(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        init(translator);
        String className = PackageManager.resolveTypeDeclaration(clazz);
        return Collections.unmodifiableSet(projectFanInMap.get(className));
    }

    @Override
    public String getName() {
        return "CBO";
    }
}
