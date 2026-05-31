package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.Type;
import gui.Updatable;
import lombok.Getter;
import translate.component.AnnotationWriter;
import translate.component.ClassWriter;
import translate.component.EnumWriter;
import translate.component.InterfaceWriter;
import translate.component.formatter.AssociationFormatter;
import translate.component.formatter.MemberFormatter;
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
    private final AssociationFormatter associationFormatter = new AssociationFormatter(formatter);


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
                        sb.append(associationFormatter.associationType(node, f, variableEntry, fieldTypeString));
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
}
