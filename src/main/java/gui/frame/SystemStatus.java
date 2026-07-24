package gui.frame;

import gui.ComplexityGridPanel;
import gui.components.JTableClass;
import gui.components.ReferenceController;
import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import org.knowm.xchart.XChartPanel;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.SystemResultAggregator;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.util.ArrayList;

public class SystemStatus extends JFrame {
    private final RadarChart chart;
    private final JPanel content = new JPanel();
    private final ComplexityTranslator translator;

    public SystemStatus(ComplexityTranslator translator) {
        super("System Status");
        this.translator = translator;
        this.chart = new RadarChartBuilder()
            .width(800)
            .height(500)
            .build();

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

        // <editor-fold desc="Charted Metrics">
        String[] names = new String[chartResults.size()];
        double[] values = new double[chartResults.size()];

        for (int i = 0; i < chartResults.size(); i++) {
            names[i] = chartResults.get(i).getName();
            values[i] = chartResults.get(i).normalize();
        }

        this.chart.setRadiiLabels(names);
        this.chart.addSeries("System", values);
        // </editor-fold>

        ComplexityGridPanel complexityGridPanel = new ComplexityGridPanel(false);
        complexityGridPanel.addMetrics(otherResults);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // finalize stuff
        var scrollPanel = new JScrollPane(content);
        var chartPanel = new XChartPanel<>(chart);

        this.content.add(chartPanel);
        this.content.add(complexityGridPanel);
        this.content.add(new ReferenceController(
            "<html><p>Classes in the system</p> <br></html>",
            translator.getClassesArray(),
            (unused) -> getClassTable()
        ));

        this.setContentPane(scrollPanel);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        FrameManager.addFrame(this);
    }

    private JPanel jclassTable = null;
    private JPanel getClassTable() {
        if (jclassTable != null) return jclassTable;

        jclassTable = new JTableClass(translator);
        return jclassTable;
    }
}
