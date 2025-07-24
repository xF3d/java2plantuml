package translate.translator;

import com.github.javaparser.ast.Node;
import gui.ComplexityGridPanel;
import translate.ClassDiagramConfig;
import translate.component.MemberFormatter;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class ComplexityTranslator implements Translator {
    public static ClassDiagramConfig config = new ClassDiagramConfig.DefaultDirector().construct();

    private Boolean error = false;
    private final Map<String, Node> map = new HashMap<>();
    private final ComplexityGridPanel output;

    public ComplexityTranslator(ComplexityGridPanel output) {
        this.output = output;
    }

    @Override
    public void addNode(Node node) {
        String fullName = MemberFormatter.fullPackageName(node);
        map.put(fullName, node);
    }

    @Override
    public void setError(Boolean b) {
        error = b;
    }

    @Override
    public void translateFile(File f) throws FileNotFoundException {

    }

    public JList<String> toComplexityList() {
        return new JList<>(map.keySet().toArray(String[]::new));
    }
}
