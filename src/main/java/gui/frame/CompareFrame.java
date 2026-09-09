package gui.frame;

import gui.components.ClassTableMetricsPanel;
import gui.components.ComplexityGridPanel;
import gui.components.ReferenceController;
import gui.components.SystemChartPanel;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class CompareFrame extends JFrame {
    private final JPanel content = new JPanel();

    public CompareFrame(
            ComplexityTranslator originalTranslator,
            ComplexityTranslator dummyTranslator
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

        this.content.add(new ReferenceController(
                "<html><p>Updated Classes in the system</p> <br></html>",
                dummyTranslator.getClassesArray(),
                (_) -> new ClassTableMetricsPanel(dummyTranslator)
        ));

        setContentPane(new JScrollPane(content));

        //JPanel testResults = new JPanel(new BorderLayout());

        //testResults.add(new TestResultPanel("Original", originalResult), BorderLayout.WEST);
        //testResults.add(new TestResultPanel("Updated", dummyResult), BorderLayout.EAST);
        //content.add(testResults);

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
