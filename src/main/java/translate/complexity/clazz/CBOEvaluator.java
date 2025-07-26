package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.MemberFormatter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CBOEvaluator implements ComplexityEvaluator.Clazz {
    private static final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
            .name("CBO")
            .max(14)
            .min(-1);

    //TODO: redo from 0, I am about to shot myself
    @Override
    public ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz) {
        int fanOut = getFQNusedBy(clazz).size();
        int fanIn = 0;

        String fqn = MemberFormatter.fullPackageName(clazz).replace('$', '.');
        for (var entry : allClazz) {
            Set<String> result = getFQNusedBy(entry);
            if (result.contains(fqn)) {
                fanIn++;
            }
        }
        System.out.println(fanOut + " : " + fanIn);

        return builder.value(fanOut - fanIn).build();
    }

    public static Set<String> getFQNusedBy(Node clazz) {
        Set<String> usedTypes = ConcurrentHashMap.newKeySet();

        // Field types
        clazz.findAll(FieldDeclaration.class).forEach(field -> {
            try {
                ResolvedType resolved = field.getElementType().resolve();
                collectReferenceTypes(resolved, usedTypes);
            } catch (Exception ignored) {
                var type = field.getElementType().asString().strip();
                if (type.equals("var")) {
                    return;
                }

                if (usedTypes.add("EXTERNAL." + type)) {
                    System.out.println("MethodDecl: '" + type + "'");
                }
            }
        });

        // Method return types
        clazz.findAll(MethodDeclaration.class).forEach(method -> {
            if (method.getType().isVarType()) {
                return;
            }

            try {
                ResolvedType resolved = method.getType().resolve();
                collectReferenceTypes(resolved, usedTypes);
            } catch (Exception ignored) {
                var type = method.getType().asString().strip();
                if (type.equals("var")) {
                    return;
                }

                if (usedTypes.add("EXTERNAL." + type)) {
                    System.out.println("MethodDecl: '" + type + "'");
                }
            }
        });

        // Local variable types
        clazz.findAll(VariableDeclarationExpr.class).forEach(var -> {
            try {
                ResolvedType resolved = var.getElementType().resolve();
                collectReferenceTypes(resolved, usedTypes);
            } catch (Exception ignored) {
                var type = var.getElementType().asString().strip();
                if (type.equals("var")) {
                    return;
                }

                if (usedTypes.add("EXTERNAL." + type)) {
                    System.out.println("MethodDecl: '" + type + "'");
                }

            }
        });

        /*
        // Static method calls (e.g., Math.max())
        clazz.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                ResolvedMethodDeclaration resolved = call.resolve();
                usedTypes.add(resolved.declaringType().getQualifiedName());
            } catch (Exception ignored) {
                if (usedTypes.add("EXTERNAL." + call.getNameAsString() + "." + call.getArguments().size())) {
                    System.out.println("MethodCall: EXTERNAL." + call.getNameAsString() + "." + call.getArguments().size());
                }
            }
        });*/

        return usedTypes;
    }

    // Handles generics and nested types (e.g., List<String>)
    private static void collectReferenceTypes(ResolvedType type, Set<String> collector) {
        if (type.isReferenceType()) {
            try {
                ResolvedReferenceType refType = type.asReferenceType();
                collector.add(refType.getQualifiedName());

                // Also process type parameters (e.g., Map<K, V>)
                for (ResolvedType tp : refType.typeParametersValues()) {
                    if (tp.isWildcard() || tp.isTypeVariable()) continue;

                    collectReferenceTypes(tp, collector);
                }
            } catch (Exception ignored) {}
        } else if (type.isArray()) {
            collectReferenceTypes(type.asArrayType().getComponentType(), collector);
        }
    }



}
