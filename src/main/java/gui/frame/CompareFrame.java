package gui.frame;

import buildapi.result.TestResult;
import gui.ComplexityGridPanel;
import gui.components.SystemChartPanel;
import gui.components.TestResultPanel;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CompareFrame extends JFrame {
    private final JPanel content = new JPanel();

    public CompareFrame(
            ComplexityTranslator originalTranslator,
            ComplexityTranslator dummyTranslator,
            TestResult originalResult,
            TestResult dummyResult
    ) {
        super("Comparing");

        var displayOriginal = originalTranslator.evaluateSystem().displayList();
        var displayDummy = dummyTranslator.evaluateSystem().displayList();

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        SystemChartPanel chartPanel = new SystemChartPanel();
        chartPanel.addProject("Original", displayOriginal[0]);
        chartPanel.addProject("Updated", displayDummy[0]);

        content.add(chartPanel);

        addGridPanel("Original metrics: ", displayOriginal[1]);
        addGridPanel("Updated metrics: ", displayDummy[1]);

        setContentPane(new JScrollPane(content));

        JPanel testResults = new JPanel(new BorderLayout());

        testResults.add(new TestResultPanel("Original", originalResult), BorderLayout.WEST);
        testResults.add(new TestResultPanel("Updated", dummyResult), BorderLayout.EAST);
        content.add(testResults);

        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

        FrameManager.addFrame(this);
    }

    private void addGridPanel(String title, List<ComplexityMetricResult> resultList) {
        content.add(new JLabel(title));

        var metricPanel = new ComplexityGridPanel();
        metricPanel.addMetrics(resultList);

        content.add(metricPanel);
    }
}
