package gui.frame;

import gui.components.ComplexityGridPanel;
import gui.components.ClassTableMetricsPanel;
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

        var results = systemMetrics.displayList();
        ArrayList<ComplexityMetricResult> chartResults = results[0];
        ArrayList<ComplexityMetricResult> otherResults = results[1];

        // </editor-fold>

        ComplexityGridPanel complexityGridPanel = new ComplexityGridPanel(false);
        complexityGridPanel.addMetrics(otherResults);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        var scrollPanel = new JScrollPane(content);

        SystemChartPanel systemChartPanel = new SystemChartPanel();
        systemChartPanel.addProject("System", chartResults);

        JPanel problematicDisplay = new JPanel();
        problematicDisplay.setLayout(new BoxLayout(problematicDisplay, BoxLayout.X_AXIS));

        problematicDisplay.add(new JLabel("Problematic classes: " + translator.getProblematicClasses().size()));
        problematicDisplay.add(new JLabel("Problematic methods: " + translator.getProblematicMethods().size()));


        this.content.add(systemChartPanel);
        this.content.add(complexityGridPanel);
        this.content.add(problematicDisplay);

        this.content.add(new ReferenceController(
            "<html><p>Classes in the system</p> <br></html>",
            translator.getClassesArray(),
            (unused) -> new ClassTableMetricsPanel(translator)
        ));

        this.setContentPane(scrollPanel);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        FrameManager.addFrame(this);
    }
}
