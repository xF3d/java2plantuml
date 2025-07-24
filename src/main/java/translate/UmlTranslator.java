package translate;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.visitor.VoidVisitor;
import translate.component.ClassWriter;
import translate.component.EnumWriter;
import translate.component.InterfaceWriter;
import translate.component.MemberFormatter;
import translate.component.RecordWriter;
import translate.component.SetTranslatingComponent;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UmlTranslator implements Translator {

    private final Set<SetTranslatingComponent<?>> componentTranslators = new HashSet<>();
    private Boolean error = false;

    public static ClassDiagramConfig config = new ClassDiagramConfig.DefaultDirector().construct();
    private final JTextArea output;

    public UmlTranslator(JTextArea outputArea) {
        output = outputArea;
        componentTranslators.add(new ClassWriter());
        componentTranslators.add(new InterfaceWriter());
        componentTranslators.add(new EnumWriter());
        componentTranslators.add(new RecordWriter());
    }

    @Override
    public void addClass(ClassOrInterfaceDeclaration c) {
        addNode(c);
    }

    @Override
    public void addEnum(EnumDeclaration e) {
        addNode(e);
    }

    @Override
    public void addInterface(ClassOrInterfaceDeclaration i) {
        addNode(i);
    }

    @Override
    public void addRecord(RecordDeclaration r) {
        addNode(r);
    }

    @Override
    public void setError(Boolean b) {
        this.error = b;
    }

    public void addNode(Node node) {
        componentTranslators.forEach((s) -> s.safeAdd(node));
    }

    @Override
    public void translateFile(File f) {
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        File file = f.getAbsoluteFile();
        try {
            ParseResult<CompilationUnit> result = parser.parse(file);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                for (VoidVisitor<Void> visitor : config.getVisitorAdapters()) {
                    cu.accept(visitor, null);
                }
            } else {
                output.append("Parsing failed for: " + file.getPath() + "\n");
                List<Problem> problems = result.getProblems();
                for (Problem problem : problems) {
                    output.append("Problem: " + problem.getMessage());
                    problem.getLocation().ifPresent(loc -> System.out.println(" at " + loc + "\n"));
                }
            }


        } catch (FileNotFoundException e) {
            setError(true);
            e.printStackTrace();
        }
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

        if (!config.isShowColoredAccessSpecifiers()) sb.append("skinparam classAttributeIconSize 0\n");

        writeAssociations(sb);

        for (var writer : componentTranslators) {
            Map<String, List<String>> map = writer.write();
            sb.append(mapWriter(map));
        }

        sb.append("@enduml");

        return sb.toString();
    }

    private String mapWriter(Map<String, List<String>> map) {
        StringBuilder builder = new StringBuilder();
        for (var entry : map.entrySet()) {
            String packageName = entry.getKey();
            if (toString().isEmpty()) {
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
