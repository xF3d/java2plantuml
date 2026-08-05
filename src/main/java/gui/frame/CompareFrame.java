package gui.frame;

import gui.ComplexityGridPanel;
import gui.components.SystemChartPanel;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.util.List;

public class CompareFrame extends JFrame {
    private final ComplexityTranslator originalTranslator;
    private final ComplexityTranslator dummyTranslator;

    public CompareFrame(ComplexityTranslator originalTranslator, ComplexityTranslator dummyTranslator) {
        super("Comparing");
        this.originalTranslator = originalTranslator;
        this.dummyTranslator = dummyTranslator;

        SystemChartPanel panel = new SystemChartPanel();

        var displayOriginal = originalTranslator.evaluateSystem().displayList();
        var displayDummy = dummyTranslator.evaluateSystem().displayList();

        panel.addProject("Original", displayOriginal[0]);
        panel.addProject("Updated", displayDummy[0]);

        super.add(panel);

        addGridPanel("Original metrics: ", displayOriginal[1]);
        addGridPanel("Updated metrics: ", displayDummy[1]);

        super.setVisible(true);
        super.setExtendedState(super.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        FrameManager.addFrame(this);
    }

    private void addGridPanel(String title, List<ComplexityMetricResult> resultList) {
        super.add(new JLabel(title));

        var metricPanel = new ComplexityGridPanel();
        metricPanel.addMetrics(resultList);

        super.add(metricPanel);
    }
}
