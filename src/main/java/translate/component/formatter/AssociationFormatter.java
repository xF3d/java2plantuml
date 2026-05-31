package translate.component.formatter;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AssociationFormatter {
    private final MemberFormatter formatter;

    /**
     * Mostly heuristic, can't be perfect
     * <br>
     * returns "--o" in case of aggregation
     * returns "--*" in case of composition
     */
    public String associationType(Node clazz, FieldDeclaration field, VariableDeclarator variableDecl, String fieldTypeString) {
        boolean isComposition = isComposition(clazz, variableDecl, fieldTypeString);

        boolean multipleAssociations = isMultiple(variableDecl);

        if (!multipleAssociations) {
            // se non è composizione è aggregazione
            if (isComposition) {
                return "--*";
            } else {
                return "--o";
            }
        } else {
            if (isComposition) {
                return "--* \"many\"";
            } else {
                return "--o \"many\"";
            }
        }
    }

    private static boolean isMultiple(VariableDeclarator variableDecl) {
        Type type = variableDecl.getType();
        if (type.isArrayType()) {
            return true;
        }

        try {
            ResolvedType resolvedType = type.resolve();
            if (resolvedType.isReferenceType()) {
                ResolvedReferenceType refType = resolvedType.asReferenceType();

                return refType.getQualifiedName().equals("java.util.Collection")
                    || refType.getAllAncestors().stream()
                    .anyMatch(a ->
                        a.getQualifiedName().equals("java.util.Collection"));
            }
        } catch (Exception ignored) {}

        return false;
    }

    private boolean isComposition(Node clazz, VariableDeclarator variableDecl, String fieldTypeString) {
        if (variableDecl.getInitializer().isPresent()) return true;

        if (!(clazz instanceof NodeWithMembers<?> members)) {
            return false;
        }

        String variableName = variableDecl.getNameAsString();
        return hasConstructor(members, fieldTypeString, variableName);
    }

    /**
     *
     * @param members class with members
     * @param fieldTypeString changed variable type in the constructor
     * @param fieldName changed variable name in the constructor
     * @return true if the constructor changes the specified field with a supplied argument
     */
    private boolean hasConstructor(NodeWithMembers<?> members, String fieldTypeString, String fieldName) {
        for (ConstructorDeclaration ctor : members.getConstructors()) {
            for (Parameter parameter : ctor.getParameters()) {
                Type type = parameter.getType();
                if (type.isPrimitiveType()) continue;

                if (formatter.fullPackageName(type).equals(fieldTypeString)) {
                    String paramName = parameter.getNameAsString();
                    if (hasAssignment(ctor, fieldName, paramName)) return true;
                    break;
                }
            }
        }

        return false;
    }

    private static boolean hasAssignment(Node node, String name, String paramName) {
        for (AssignExpr assignExpr : node.findAll(AssignExpr.class)) {
            if (assignExpr.getOperator() != AssignExpr.Operator.ASSIGN) continue;

            // check if value passed is the same name as the paramName
            Expression valueExpr = assignExpr.getValue();
            if (!valueExpr.isNameExpr()) continue;

            if (!valueExpr.asNameExpr().getNameAsString().equals(paramName)) continue;

            // check if target is correctly the field
            Expression target = assignExpr.getTarget();
            if (target.isFieldAccessExpr()) {
                FieldAccessExpr fieldAccessExpr = target.asFieldAccessExpr();
                Expression scope = fieldAccessExpr.getScope();
                String fieldAccessName = fieldAccessExpr.getNameAsString();

                if (scope == null && fieldAccessName.equals(name)) {
                    return true;
                }

                if (scope == null) continue;

                if (scope.isThisExpr() && fieldAccessName.equals(name)) {
                    return true;
                }
            }

            // check if it is the field but without "this"
            if (target.isNameExpr()) {
                NameExpr nameExpr = target.asNameExpr();
                if (nameExpr.getNameAsString().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
