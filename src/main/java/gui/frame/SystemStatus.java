package gui.frame;

import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import org.knowm.xchart.SwingWrapper;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import javax.swing.*;

public class SystemStatus extends JFrame {
    private final RadarChart chart;

    public SystemStatus(ComplexityTranslator translator) {
        super("System Status");
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        this.chart = new RadarChartBuilder()
            .width(800)
            .height(500)
            .build();

        ComplexityMetricResult[] results = translator.evaluateSystem();


        String[] names = new String[results.length];
        double[] values = new double[results.length];

        for (int i = 0; i < results.length; i++) {
            names[i] = results[i].getName();
            values[i] = results[i].normalize();
        }

        this.chart.setRadiiLabels(names);
        this.chart.addSeries("System", values);

        new SwingWrapper<>(chart).displayChart();
    }
}
