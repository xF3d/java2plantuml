package translate.complexity.clazz;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import java.util.*;

/**
 * Measures how closely related the methods of a class are based on the instance variables they share.
 * High LCOM indicates poor cohesion, suggesting the class performs multiple unrelated responsibilities and may benefit from being split into smaller classes.
 */
public class LCOM1Evaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("LCOM1");

        int p = 0;
        int q = 0;


        Set<String> fields = new HashSet<>();
        for (var field : clazz.getFields()) {
            if (field.isStatic()) continue;

            for (var variable : field.getVariables()) {
                fields.add(variable.getNameAsString());
            }
        }

        if (fields.isEmpty()) {
            return builder.value(0).max(1).min(-1).build();
        }

        List<MethodDeclaration> definedMethods = clazz.getMethods().stream()
                .filter(it -> it.getBody().isPresent())
                .toList();
        // Total method pairs = n * (n-1) / 2 (for n methods)
        int totalPairs = definedMethods.size() * (definedMethods.size() - 1) / 2;

        for (int i = 0; i < definedMethods.size() - 1; i++) {
            MethodDeclaration subject = definedMethods.get(i);
            Optional<BlockStmt> subjectOptBlock = subject.getBody();

            // should never happen
            if (subjectOptBlock.isEmpty()) {
                continue;
            }

            Set<String> subjectAccesses = collectInstanceVariableAccesses(subjectOptBlock.get(), fields);

            for (int j = i + 1; j < definedMethods.size(); j++) {
                MethodDeclaration other = definedMethods.get(j);
                Optional<BlockStmt> otherOptBlock = other.getBody();
                if (otherOptBlock.isEmpty()) {
                    continue;
                }

                Set<String> otherAccesses = collectInstanceVariableAccesses(otherOptBlock.get(), fields);

                Set<String> temp = new HashSet<>(subjectAccesses);
                temp.retainAll(otherAccesses);

                if (temp.isEmpty()) {
                    p++; // does not share instance variables
                } else {
                    q++; // does share instance variables
                }
            }
        }


        return builder
                .max(totalPairs)
                .min(-1)
                .value(Math.max(p - q, 0))
                .build();
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
    public String getName() {
        return "LCOM1";
    }

    @Override
    public boolean usefulForSystem() {
        return false;
    }
}
