package gui.frame;

import gui.ComplexityGridPanel;
import gui.components.JTableClass;
import gui.components.ReferenceController;
import gui.components.SystemChartPanel;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.SystemResultAggregator;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.util.ArrayList;

public class SystemStatus extends JFrame {
    private final JPanel content = new JPanel();
    private final ComplexityTranslator translator;

    public SystemStatus(ComplexityTranslator translator) {
        super("System Status");
        this.translator = translator;

        SystemResultAggregator systemMetrics = translator.evaluateSystem();

        ArrayList<ComplexityMetricResult> allResults = new ArrayList<>(systemMetrics.getSystemMetrics().values());
        allResults.addAll(systemMetrics.getClassMetrics().values());

        ArrayList<ComplexityMetricResult> chartResults = new ArrayList<>();
        ArrayList<ComplexityMetricResult> otherResults = new ArrayList<>();
        for (var result : allResults) {
            if (result.isThresholdValid()) {
                chartResults.add(result);
            }

            otherResults.add(result);
        }

        // </editor-fold>

        ComplexityGridPanel complexityGridPanel = new ComplexityGridPanel(false);
        complexityGridPanel.addMetrics(otherResults);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        var scrollPanel = new JScrollPane(content);

        SystemChartPanel systemChartPanel = new SystemChartPanel();
        systemChartPanel.addProject("System", chartResults);
        this.content.add(systemChartPanel);

        this.content.add(complexityGridPanel);
        this.content.add(new ReferenceController(
            "<html><p>Classes in the system</p> <br></html>",
            translator.getClassesArray(),
            (unused) -> new JTableClass(translator)
        ));

        this.setContentPane(scrollPanel);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        FrameManager.addFrame(this);
    }
}
