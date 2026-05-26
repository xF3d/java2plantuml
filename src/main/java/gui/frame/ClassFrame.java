package gui.frame;

import gui.ComplexityGridPanel;
import gui.jlist.JListReferenced;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

        super.getContentPane().add(gridInfo);
        super.getContentPane().add(methodScroll);
        super.setVisible(true);

        FrameManager.addFrame(this);
    }

    private JScrollPane getJScrollPane(ComplexityTranslator umlTranslator) {
        var complexityList = new JListReferenced(umlTranslator.getMethodsJList(className)) {
            @Override
            public void onClick(String selectedItem) {
                new MethodFrame(className, selectedItem, umlTranslator);
            }
        };

        return new JScrollPane(complexityList);
    }
}
