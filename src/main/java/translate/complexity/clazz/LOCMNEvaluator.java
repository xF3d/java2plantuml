package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class LOCMNEvaluator implements ComplexityEvaluator.Clazz {
    private static ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
            .name("LOCMN");

    @Override
    public ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz) {
        int p = 0;
        int q = 0;

        // no point in doing any LOCM evaluation in this case
        if (!(clazz instanceof NodeWithMembers<?> nwm)) {
            return builder.value(0).build();
        }


        Set<String> fields = new HashSet<>();
        for (var field : nwm.getFields()) {
            for (var variable : field.getVariables()) {
                fields.add(variable.getNameAsString());
            }
        }



        List<MethodDeclaration> methods = nwm.getMethods();
        // Total method pairs = n * (n-1) / 2 (for n methods)
        int totalPairs = methods.size() * (methods.size() - 1) / 2;

        for (int i = 0; i < methods.size() - 1; i++) {
            MethodDeclaration subject = methods.get(i);
            Optional<BlockStmt> subjectOptBlock = subject.getBody();
            if (subjectOptBlock.isEmpty()) {
                continue;
            }

            Set<String> subjectAccesses = collectInstanceVariableAccesses(subjectOptBlock.get(), fields);

            for (int j = i + 1; j < methods.size(); j++) {
                MethodDeclaration other = methods.get(j);
                Optional<BlockStmt> otherOptBlock = other.getBody();
                if (otherOptBlock.isEmpty()) {
                    continue;
                }

                Set<String> otherAccesses = collectInstanceVariableAccesses(otherOptBlock.get(), fields);

                Set<String> temp = new HashSet<>(subjectAccesses);
                temp.removeAll(otherAccesses);

                if (temp.isEmpty()) {
                    p++; // does not share instance variables
                } else {
                    q++; // does share instance variables
                }
            }
        }


        return builder
                .max(totalPairs)
                .min(-totalPairs)
                .value(p - q)
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

}
