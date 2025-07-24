package translate.translator;

import com.github.javaparser.ast.Node;
import translate.ClassDiagramConfig;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.clazz.DITEvaluator;
import translate.component.MemberFormatter;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class ComplexityTranslator implements Translator {
    public static ClassDiagramConfig config = new ClassDiagramConfig.DefaultDirector().construct();

    private Boolean error = false;
    private final Map<String, Node> map = new HashMap<>();
    private final ComplexityEvaluator.Clazz[] evaluators = {
            new DITEvaluator()
    };

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
    public ClassDiagramConfig getConfig() {
        return config;
    }

    public JList<String> toComplexityList() {
        return new JList<>(map.keySet().toArray(String[]::new));
    }
}
