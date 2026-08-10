package gui.frame;

import gui.ComplexityGridPanel;
import gui.components.MethodTableMetricsPanel;
import gui.components.ReferenceController;
import translate.translator.ComplexityTranslator;

import javax.swing.*;

public class ClassFrame extends JFrame {
    private final ComplexityGridPanel gridInfo = new ComplexityGridPanel();

    public ClassFrame(String className, ComplexityTranslator translator) {
        super(className);
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        var content = new JPanel();
        var scrollPanel = new JScrollPane(content);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        var evaluation = translator.evaluateClass(className);
        gridInfo.addMetrics(evaluation);

        JLabel smellList = new JLabel("Code Smells: " + String.join(", ", evaluation.getCodeSmells()));
        content.add(smellList);
        content.add(new ReferenceController(
                "<html><p>Methods in the class</p> <br></html>",
                translator.getMethodsArray(className),
                (unused) -> new MethodTableMetricsPanel(className, translator)
        ));

        super.setContentPane(scrollPanel);

        super.setVisible(true);
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);

        FrameManager.addFrame(this);
    }
}
