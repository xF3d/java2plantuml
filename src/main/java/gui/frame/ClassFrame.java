package gui.frame;

import gui.ComplexityGridPanel;
import gui.components.JListReferenced;
import translate.translator.ComplexityTranslator;

import javax.swing.*;

public class ClassFrame extends JFrame {
    private final String className;
    private final ComplexityGridPanel gridInfo = new ComplexityGridPanel();
    private final JScrollPane methodScroll;

    public ClassFrame(String className, ComplexityTranslator translator) {
        super(className);
        this.className = className;
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        super.getContentPane().setLayout(new BoxLayout(super.getContentPane(), BoxLayout.Y_AXIS));

        var evaluation = translator.evaluateClass(className);
        gridInfo.addMetrics(evaluation);

        methodScroll = getJScrollPane(translator);
        JLabel smellList = new JLabel("Code Smells: " + String.join(", ", evaluation.getCodeSmells()));

        super.getContentPane().add(smellList);
        super.getContentPane().add(gridInfo);
        super.getContentPane().add(methodScroll);
        super.setVisible(true);

        FrameManager.addFrame(this);
    }

    private JScrollPane getJScrollPane(ComplexityTranslator umlTranslator) {
        var complexityList = new JListReferenced(umlTranslator.getMethodsArray(className)) {
            @Override
            public void onClick(String selectedItem) {
                new MethodFrame(className, selectedItem, umlTranslator);
            }
        };

        return new JScrollPane(complexityList);
    }
}
