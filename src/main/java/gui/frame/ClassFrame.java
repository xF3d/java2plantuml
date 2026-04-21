package gui.frame;

import gui.ComplexityGridPanel;
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
        JList<String> complexityList = umlTranslator.getMethodsJList(className);
        complexityList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) { // double-click
                    return;
                }

                int index = complexityList.locationToIndex(e.getPoint());
                if (index == -1) {
                    return;
                }

                String selectedItem = complexityList.getModel().getElementAt(index);
                new MethodFrame(className, selectedItem, umlTranslator);
            }
        });

        return new JScrollPane(complexityList);
    }
}
