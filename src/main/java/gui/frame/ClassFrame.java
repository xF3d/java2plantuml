package gui.frame;

import gui.ComplexityGridPanel;
import translate.translator.ComplexityTranslator;

import javax.swing.*;

public class ClassFrame extends JFrame {
    private final ComplexityGridPanel gridInfo = new ComplexityGridPanel();
    private final JScrollPane methodScroll;

    public ClassFrame(String className, ComplexityTranslator translator) {
        super(className);
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        super.getContentPane().setLayout(new BoxLayout(super.getContentPane(), BoxLayout.Y_AXIS));

        var evaluation = translator.evaluateClass(className);
        gridInfo.addMetrics(evaluation);

        methodScroll = new JScrollPane(translator.getMethodsJList(className));

        super.getContentPane().add(gridInfo);
        super.getContentPane().add(methodScroll);
        super.setVisible(true);

        FrameManager.addFrame(this);
    }
}
