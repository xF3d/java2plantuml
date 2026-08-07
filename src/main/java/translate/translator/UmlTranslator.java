package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.Type;
import file.Config;
import file.FileLogger;
import lombok.Getter;
import translate.component.*;
import translate.component.formatter.AssociationFormatter;
import translate.component.formatter.MemberFormatter;
import translate.structure.pkg.PackageManager;

import java.io.File;
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

    private boolean executed;

    private String calculatedPlantuml = null;

    public UmlTranslator() {
        componentTranslators.add(new ClassWriter());
        componentTranslators.add(new InterfaceWriter());
        componentTranslators.add(new EnumWriter());
        componentTranslators.add(new RecordWriter());
        componentTranslators.add(new AnnotationWriter());
    }

    private final Set<String> printed = new HashSet<>();
    @Override
    public void addNode(Node node) {
        String type = formatter.nodeClassType(node);
        String name = formatter.fullPackageName(node);

        // Class/Interfaces processed twice
        if (printed.add(name)) {
            FileLogger.LOGGER.info(type + " Found: " + name + "\n");
        }

        componentTranslators.forEach((s) -> s.safeAdd(node));
    }

    @Override
    public boolean execute(File selectedDirectory) {
        boolean result = Translator.super.execute(selectedDirectory);
        executed = true;
        return result;
    }

    @Override
    public boolean hasExecuted() {
        return executed;
    }

    public String toPlantUml() {
        if (calculatedPlantuml != null) return calculatedPlantuml;

        StringBuilder sb = new StringBuilder();

        sb.append("@startuml");
        sb.append("\nset separator " + MemberFormatter.PACKAGE_DELIMITER);
        sb.append("\n");
        //this is for removing shapes in attributes/methods visibility

        if (!Config.getInstance().getConfig().isShowColoredAccessSpecifiers()) sb.append("skinparam classAttributeIconSize 0\n");


        Set<UmlAssociation> associations = new HashSet<>();
        for (var writer : componentTranslators) {
            var result = writer.writeUML(formatter);
            sb.append(mapWriter(result.packageMap()));
            associations.addAll(result.associations());
        }

        var filter = Config.getInstance().getConfig().getPackageFilter();
        associations.forEach(it -> {
            if (filter.isValid(it.from()) && filter.isValid(it.to())) {
                sb.append(it).append("\n");
            }
        });

        initFqn();

        if (Config.getInstance().getConfig().isShowAssociations()) {
            writeAssociations(sb);
        }

        sb.append("@enduml");

        this.calculatedPlantuml = sb.toString();

        return calculatedPlantuml;
    }

    private void initFqn() {
        fqn.clear();
        var filter = Config.getInstance().getConfig().getPackageFilter();
        for (SetTranslatingComponent<?> entry : componentTranslators) {
            for (Node r : entry.getSet()) {
                String name = formatter.fullPackageName(r.findCompilationUnit().orElseThrow(), r);
                if (filter.isValid(name)) fqn.add(name);
            }
        }
    }

    private String mapWriter(Map<String, List<String>> map) {
        StringBuilder builder = new StringBuilder();
        var filter = Config.getInstance().getConfig().getPackageFilter();
        for (var entry : map.entrySet()) {
            String packageName = entry.getKey();
            if (!filter.isValid(packageName)) continue;

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
            if (entry instanceof InterfaceWriter) continue;

            if (!NodeWithMembers.class.isAssignableFrom(entry.type())) continue;

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
