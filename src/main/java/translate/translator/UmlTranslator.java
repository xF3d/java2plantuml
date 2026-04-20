package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import gui.Updatable;
import translate.ClassDiagramConfig;
import translate.component.ClassWriter;
import translate.component.EnumWriter;
import translate.component.InterfaceWriter;
import translate.component.MemberFormatter;
import translate.component.RecordWriter;
import translate.component.SetTranslatingComponent;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UmlTranslator implements Translator {

    private final Set<SetTranslatingComponent<?>> componentTranslators = new HashSet<>();
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
    }

    @Override
    public void setError(Boolean b) {
        this.error = b;
    }

    private final Set<String> printed = new HashSet<>();
    @Override
    public void addNode(Node node) {
        String type = MemberFormatter.nodeClassType(node);
        String name = MemberFormatter.fullPackageName(node);

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
        sb.append("\n");
        //this is for removing shapes in attributes/methods visibility

        if (!TranslatorConfig.config.isShowColoredAccessSpecifiers()) sb.append("skinparam classAttributeIconSize 0\n");

        writeAssociations(sb);

        for (var writer : componentTranslators) {
            Map<String, List<String>> map = writer.writeUML();
            sb.append(mapWriter(map));
        }

        sb.append("@enduml");

        return sb.toString();
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
        HashSet<String> temp = new HashSet<>();

        for (SetTranslatingComponent<?> entry : componentTranslators) {
            for (Node r : entry.getSet()) {
                temp.add(MemberFormatter.fullSimpleName(r));
            }
        }

        for (SetTranslatingComponent<?> entry : componentTranslators) {
            if (!entry.type().isAssignableFrom(NodeWithMembers.class)) {
                continue;
            }

            for (Node node : entry.getSet()) {

                if (!(node instanceof NodeWithMembers<?> c)) {
                    break;
                }

                for (FieldDeclaration f : c.getFields()) {

                    if (!temp.contains(f.getVariables().get(0).getType().asString())) {
                        continue;
                    }

                    sb.append(MemberFormatter.fullSimpleName(node));
                    sb.append("--");
                    //                    sb.append("\"-");
                    sb.append("\"");
                    sb.append(MemberFormatter.modifiers(f.getModifiers()));
                    sb.append(f.getVariables().get(0).getName());
                    sb.append("\" ");
                    sb.append(f.getVariables().get(0).getType().asString());
                    sb.append("\n");

                }

            }
        }

    }
}
