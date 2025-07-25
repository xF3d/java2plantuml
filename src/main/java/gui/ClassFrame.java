package gui;

import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.awt.*;

public class ClassFrame extends JFrame {
    private final ComplexityGridPanel gridInfo = new ComplexityGridPanel();
    private final JScrollPane methodScroll;

    public ClassFrame(String className, ComplexityTranslator translator) {
        super(className);
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        super.setLayout(new GridLayout(2, 1));

        var evaluation = translator.evaluateClass(className);
        gridInfo.addMetrics(evaluation);

        methodScroll = new JScrollPane(translator.getMethodsJList(className));

        super.getContentPane().add(gridInfo);
        super.getContentPane().add(methodScroll);
        super.setVisible(true);
    }
}
