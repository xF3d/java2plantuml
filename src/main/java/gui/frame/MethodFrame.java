package gui.frame;

import gui.ComplexityGridPanel;
import translate.translator.ComplexityTranslator;

import javax.swing.*;

public class MethodFrame extends JFrame {
    private final ComplexityGridPanel gridInfo = new ComplexityGridPanel();

    public MethodFrame(String className, String methodName, ComplexityTranslator translator) {
        super(className);
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        super.getContentPane().setLayout(new BoxLayout(super.getContentPane(), BoxLayout.Y_AXIS));

        var evaluation = translator.evaluateMethod(className, methodName);
        gridInfo.addMetrics(evaluation);

        super.getContentPane().add(gridInfo);
        super.setVisible(true);

        FrameManager.addFrame(this);
    }
}
