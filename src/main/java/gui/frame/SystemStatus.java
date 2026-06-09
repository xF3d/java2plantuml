package gui.frame;

import gui.components.JTableClass;
import gui.components.ReferenceController;
import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import org.knowm.xchart.XChartPanel;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import javax.swing.*;

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

        ComplexityMetricResult[] results = translator.evaluateSystem().getRawData();

        String[] names = new String[results.length];
        double[] values = new double[results.length];

        for (int i = 0; i < results.length; i++) {
            names[i] = results[i].getName();
            values[i] = results[i].normalize();
        }

        this.chart.setRadiiLabels(names);
        this.chart.addSeries("System", values);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        var scrollPanel = new JScrollPane(content);
        var chartPanel = new XChartPanel<>(chart);

        this.content.add(chartPanel);
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
