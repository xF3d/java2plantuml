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

        var displayOriginal = originalTranslator.evaluateSystem().displayList();
        var displayDummy = dummyTranslator.evaluateSystem().displayList();

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        SystemChartPanel chartPanel = new SystemChartPanel();
        chartPanel.addProject("Original", displayOriginal[0]);
        chartPanel.addProject("Updated", displayDummy[0]);

        content.add(chartPanel);

        content.add(new JLabel("Original metrics:"));
        ComplexityGridPanel originalPanel = new ComplexityGridPanel();
        originalPanel.addMetrics(displayOriginal[1]);
        content.add(originalPanel);

        content.add(new JLabel("Updated metrics:"));
        ComplexityGridPanel updatedPanel = new ComplexityGridPanel();
        updatedPanel.addMetrics(displayDummy[1]);
        content.add(updatedPanel);

        setContentPane(new JScrollPane(content));

        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

        FrameManager.addFrame(this);
    }

    private void addGridPanel(String title, List<ComplexityMetricResult> resultList) {
        super.add(new JLabel(title));

        var metricPanel = new ComplexityGridPanel();
        metricPanel.addMetrics(resultList);

        super.add(metricPanel);
    }
}
