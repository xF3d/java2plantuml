package gui.components;

import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import org.knowm.xchart.XChartPanel;
import translate.complexity.ComplexityMetricResult;

import javax.swing.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class SystemChartPanel extends JPanel {
    private final HashMap<String, List<ComplexityMetricResult>> projectMetrics = new HashMap<>();

    public void addProject(String name, List<ComplexityMetricResult> chartResults) {
        projectMetrics.put(name, chartResults);
        updateChart();
    }

    public void updateChart() {
        if (isEmpty()) return;
        this.removeAll();

        RadarChart chart = new RadarChartBuilder()
                .width(800)
                .height(500)
                .build();

        for (var entry : projectMetrics.entrySet()) {
            String name = entry.getKey();
            List<ComplexityMetricResult> metrics = entry.getValue();


            String[] names = new String[metrics.size()];
            double[] values = new double[metrics.size()];

            for (int i = 0; i < metrics.size(); i++) {
                var metric = metrics.get(i);
                names[i] = metric.metric();
                values[i] = metric.normalize();
            }

            chart.setRadiiLabels(names);
            chart.addSeries(name, values);
        }

        var chartPanel = new XChartPanel<>(chart);
        this.add(chartPanel);
    }

    public boolean isEmpty() {
        return projectMetrics.values().stream().allMatch(Collection::isEmpty);
    }
}
