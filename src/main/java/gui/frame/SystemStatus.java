package gui.frame;

import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import org.knowm.xchart.SwingWrapper;
import translate.complexity.ComplexityMetricResult;
import translate.translator.ComplexityTranslator;

import javax.swing.*;

public class SystemStatus {
    private final RadarChart chart;
    private final JFrame frame;

    public SystemStatus(ComplexityTranslator translator) {
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

        this.frame = new SwingWrapper<>(chart).displayChart();
        this.frame.setTitle("System Status");
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        FrameManager.addFrame(this.frame);
    }
}
