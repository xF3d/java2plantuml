package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import java.util.*;
/**
 * Measures how closely related the methods of a class are based on the instance variables they share.
 * High LCOM indicates poor cohesion, suggesting the class performs multiple unrelated responsibilities and may benefit from being split into smaller classes.
 * Henderson-Sellers LCOM*
 */
public class LCOMStarEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        Set<String> fields = new HashSet<>();
        for (var field : clazz.getFields()) {
            if (field.isStatic()) continue;

            for (var variable : field.getVariables()) {
                fields.add(variable.getNameAsString());
            }
        }

        if (fields.isEmpty()) {
            return result(0);
        }

        // Only defined methods
        List<MethodDeclaration> definedMethods = clazz.getMethods().stream()
                .filter(it -> it.getBody().isPresent())
                .toList();

        int p = definedMethods.size();
        if (p <= 1) {
            return result(0);
        }

        // Track how many methods access each field
        // Field -> Number of distinct methods accessing it
        Map<String, Integer> fieldAccessCounts = new HashMap<>();
        for (String field : fields) {
            fieldAccessCounts.put(field, 0);
        }

        for (MethodDeclaration method : definedMethods) {
            BlockStmt body = method.getBody().orElseThrow();

            Set<String> accessedInMethod = collectInstanceVariableAccesses(body, fields);
            for (String fieldName : accessedInMethod) {
                fieldAccessCounts.put(fieldName, fieldAccessCounts.get(fieldName) + 1);
            }
        }

        double totalAccesses = 0;
        for (int count : fieldAccessCounts.values()) {
            totalAccesses += count;
        }
        double q = totalAccesses / fields.size();

        double lcomStar = (p - q) / (p - 1);

        //TODO rivedere soglia
        return result(lcomStar);
    }

    public Set<String> collectInstanceVariableAccesses(BlockStmt blockStmt, Set<String> instanceVariableNames) {
        Set<String> accessedInstanceFields = new HashSet<>();

        blockStmt.accept(new VoidVisitorAdapter<Void>() {

            @Override
            public void visit(NameExpr n, Void arg) {
                String name = n.getNameAsString();
                // If the name matches an instance field, it must be an unqualified instance variable access
                if (instanceVariableNames.contains(name)) {
                    accessedInstanceFields.add(name);
                }

                super.visit(n, arg);
            }

            @Override
            public void visit(FieldAccessExpr fae, Void arg) {
                String fieldName = fae.getNameAsString();

                // Check if this is an instance variable access:
                // qualifier must be `this` (e.g. this.field)
                if (instanceVariableNames.contains(fieldName)) {
                    if (fae.getScope() instanceof ThisExpr) {
                        accessedInstanceFields.add(fieldName);
                    }
                }

                super.visit(fae, arg);
            }

        }, null);

        return accessedInstanceFields;
    }

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("LCOM*");
    }
}
