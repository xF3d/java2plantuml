package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.Type;
import gui.Updatable;
import lombok.Getter;
import translate.component.AnnotationWriter;
import translate.component.ClassWriter;
import translate.component.EnumWriter;
import translate.component.InterfaceWriter;
import translate.component.MemberFormatter;
import translate.component.RecordWriter;
import translate.component.SetTranslatingComponent;
import translate.structure.PackageManager;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UmlTranslator implements Translator {

    @Getter
    private final PackageManager packageManager = new PackageManager();

    private final MemberFormatter formatter = new MemberFormatter(packageManager);

    private final Set<SetTranslatingComponent<?>> componentTranslators = new HashSet<>();
    private final Set<String> fqn = new HashSet<>();
    private Boolean error = false;

    private final JTextArea output;
    private final Updatable updatable;

    public UmlTranslator(JTextArea outputArea, Updatable updatable) {
        output = outputArea;
        this.updatable = updatable;
        componentTranslators.add(new ClassWriter());
        componentTranslators.add(new InterfaceWriter());
        componentTranslators.add(new EnumWriter());
        componentTranslators.add(new RecordWriter());
        componentTranslators.add(new AnnotationWriter());
    }

    @Override
    public void setError(Boolean b) {
        this.error = b;
    }

    private final Set<String> printed = new HashSet<>();
    @Override
    public void addNode(Node node) {
        String type = formatter.nodeClassType(node);
        String name = formatter.fullPackageName(node);

        // Class/Interfaces processed twice
        if (printed.add(name)) {
            output.append(type + " Found: " + name + "\n");
            updatable.update();
        }

        componentTranslators.forEach((s) -> s.safeAdd(node));
    }

    public String toPlantUml() {

        StringBuilder sb = new StringBuilder();

        if (error) {
            sb.append("Error occured while parsing.");
            return sb.toString();
        }

        sb.append("@startuml");
        sb.append("\nset separator " + MemberFormatter.PACKAGE_DELIMITER);
        sb.append("\n");
        //this is for removing shapes in attributes/methods visibility

        if (!TranslatorConfig.config.isShowColoredAccessSpecifiers()) sb.append("skinparam classAttributeIconSize 0\n");


        Set<String> associations = new HashSet<>();
        for (var writer : componentTranslators) {
            var result = writer.writeUML(formatter);
            sb.append(mapWriter(result.packageMap()));
            associations.addAll(result.associations());
        }

        associations.forEach(sb::append);

        initFqn();
        writeAssociations(sb);

        sb.append("@enduml");

        return sb.toString();
    }

    private void initFqn() {
        fqn.clear();
        for (SetTranslatingComponent<?> entry : componentTranslators) {
            for (Node r : entry.getSet()) {
                fqn.add(formatter.fullPackageName(r.findCompilationUnit().get(), r));
            }
        }
    }

    private String mapWriter(Map<String, List<String>> map) {
        StringBuilder builder = new StringBuilder();
        for (var entry : map.entrySet()) {
            String packageName = entry.getKey();
            if (packageName.isEmpty()) {
                entry.getValue().forEach(builder::append);
                continue;
            }

            builder.append("package ").append(packageName).append(" {\n");
            entry.getValue().forEach(builder::append);
            builder.append("}\n");
        }

        return builder.toString();
    }

    private void writeAssociations(StringBuilder sb) {
        for (SetTranslatingComponent<?> entry : componentTranslators) {
            if (!NodeWithMembers.class.isAssignableFrom(entry.type())) {
                continue;
            }

            for (Node node : entry.getSet()) {

                // should never happen
                if (!(node instanceof NodeWithMembers<?> c)) {
                    break;
                }

                String nodeFQN = formatter.fullPackageName(node);
                for (FieldDeclaration f : c.getFields()) {
                    if (f.isStatic()) continue;

                    Type type = f.getVariables().get(0).getType();
                    if (type.isPrimitiveType()) continue;

                    String fieldTypeString = formatter.fullPackageName(type);

                    // not one of our classes, we can ignore it
                    if (!fqn.contains(fieldTypeString)) continue;

                    // remove circular arrows
                    if (nodeFQN.equals(fieldTypeString)) continue;

                    for (var variableEntry : f.getVariables()) {
                        String variableName = variableEntry.getNameAsString();
                        sb.append(nodeFQN);
                        sb.append(associationType(node, f, variableEntry, fieldTypeString));
                        sb.append("\"");
                        sb.append(formatter.modifiers(f.getModifiers()));
                        sb.append(variableName);
                        sb.append("\" ");
                        sb.append(fieldTypeString);
                        sb.append("\n");
                    }
                }
            }
        }
    }


    /**
     * Mostly heuristic, can't be perfect
     * <br>
     * returns "--o" in case of aggregation
     * returns "--*" in case of composition
     * returns "--" (dependency) in case of failure
     */
    private String associationType(Node clazz, FieldDeclaration field, VariableDeclarator variableDecl, String fieldTypeString) {
        if (!(clazz instanceof NodeWithMembers<?> members)) {
            return "--";
        }

        String variableName = variableDecl.getNameAsString();
        boolean hasSetter = hasSetter(field, members, fieldTypeString, variableName);
        boolean hasGetter = hasGetter(members, fieldTypeString, variableName);
        boolean hasConstructor = hasConstructor(members, fieldTypeString, variableName);

        boolean isComposition = isComposition(field, variableDecl, hasGetter, hasSetter, hasConstructor);
        boolean isAggregation = isAggregation(field, hasGetter, hasSetter, hasConstructor);

        // no conflicts yet but better safe than sorry in case I add more heuristics
        if (isAggregation && isComposition) {
            return "--";
        } else if (isAggregation) {
            return "--o";
        } else if (isComposition) {
            return "--*";
        }

        return "--";
    }

    private boolean isComposition(FieldDeclaration field, VariableDeclarator variableDecl, boolean hasGetter, boolean hasSetter, boolean hasConstructor) {
        if (variableDecl.getInitializer().isPresent()) return true;

        return false;
        /*
        // if public then it can be modified easily
        if (field.isPublic()) return false;

        // there must be no setters
        if (hasSetter || hasConstructor) return false;

        // there must be no getters
        return !hasGetter;*/
    }


    private boolean isAggregation(FieldDeclaration field, boolean hasGetter, boolean hasSetter, boolean hasConstructor) {
        return false;
        /*if (!hasConstructor && !hasSetter) return false;

        return hasGetter;*/
    }

    private boolean hasConstructor(NodeWithMembers<?> members, String fieldTypeString, String fieldName) {
        for (ConstructorDeclaration ctor : members.getConstructors()) {
            boolean invalidCtor = true;
            String paramName = null;
            for (Parameter parameter : ctor.getParameters()) {
                Type type = parameter.getType();
                if (type.isPrimitiveType()) continue;

                if (formatter.fullPackageName(type).equals(fieldTypeString)) {
                    invalidCtor = false;
                    paramName = parameter.getNameAsString();
                    break;
                }
            }

            if (invalidCtor) continue;

            if (hasAssignment(fieldName, ctor, paramName)) return true;
        }

        return false;
    }

    private boolean hasGetter(NodeWithMembers<?> methodHolder, String fieldTypeString, String fieldName) {
        for (MethodDeclaration method : methodHolder.getMethods()) {
            if (!formatter.fullPackageName(method.getType()).equals(fieldTypeString)) {
                continue;
            }

            for (ReturnStmt retStmt : method.findAll(ReturnStmt.class)) {
                var expOptional = retStmt.getExpression();
                if (expOptional.isEmpty()) continue;

                Expression exp = expOptional.get();

                if (exp.isFieldAccessExpr()) {
                    FieldAccessExpr fieldAccessExpr = exp.asFieldAccessExpr();
                    if (fieldAccessExpr.getScope().isThisExpr() &&
                        fieldAccessExpr.getNameAsString().equals(fieldName)) {
                        return true;
                    }
                }

                if (exp.isNameExpr()) {
                    NameExpr nameExpr = exp.asNameExpr();
                    if (nameExpr.getNameAsString().equals(fieldName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean hasSetter(FieldDeclaration field, NodeWithMembers<?> methodHolder, String fieldTypeString, String name) {
        if (field.isFinal()) return false;

        for (MethodDeclaration method : methodHolder.getMethods()) {
            boolean invalidMethod = true;
            String paramName = null;
            for (Parameter parameter : method.getParameters()) {
                Type type = parameter.getType();
                if (type.isPrimitiveType()) continue;

                if (formatter.fullPackageName(type).equals(fieldTypeString)) {
                    invalidMethod = false;
                    paramName = parameter.getNameAsString();
                    break;
                }
            }

            if (invalidMethod) continue;

            if (hasAssignment(name, method, paramName)) return true;
        }

        return false;
    }

    private static boolean hasAssignment(String name, Node methodOrCtor, String paramName) {
        for (AssignExpr assignExpr : methodOrCtor.findAll(AssignExpr.class)) {
            if (assignExpr.getOperator() != AssignExpr.Operator.ASSIGN) continue;

            Expression valueExpr = assignExpr.getValue();
            if (!valueExpr.isNameExpr()) continue;

            if (!valueExpr.asNameExpr().getNameAsString().equals(paramName)) continue;

            Expression target = assignExpr.getTarget();
            if (target.isFieldAccessExpr()) {
                FieldAccessExpr fieldAccessExpr = target.asFieldAccessExpr();
                if (fieldAccessExpr.getScope().isThisExpr() &&
                    fieldAccessExpr.getNameAsString().equals(name)) {
                    return true;
                }
            }

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
