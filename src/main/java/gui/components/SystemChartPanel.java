package gui.components;

import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import org.knowm.xchart.XChartPanel;
import translate.complexity.result.BoundedResult;
import translate.complexity.result.ComplexityMetricResult;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class SystemChartPanel extends JPanel {
    private final HashMap<String, List<BoundedResult>> projectMetrics = new HashMap<>();

    public void addProject(String name, List<BoundedResult> chartResults) {
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
            List<BoundedResult> metrics = entry.getValue();


            String[] names = new String[metrics.size()];
            double[] values = new double[metrics.size()];
            String[] toolTips = new String[metrics.size()];


            for (int i = 0; i < metrics.size(); i++) {
                var metric = metrics.get(i);
                names[i] = metric.getDefinition().metric();
                values[i] = metric.normalize();
                toolTips[i] = metric.getDefinition().printableThreshold();
            }

            chart.setRadiiLabels(names);
            chart.addSeries(name, values, toolTips);
        }

        var styler = chart.getStyler();
        styler.setToolTipsEnabled(true);

        if (projectMetrics.size() == 1) {
            styler.setLegendVisible(false);
        }

        var chartPanel = new XChartPanel<>(chart);
        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (projectMetrics.size() == 1) return;

                var seriesMap = chart.getSeriesMap();
                var newSeries = shiftRight(seriesMap);

                seriesMap.clear();
                seriesMap.putAll(newSeries);

                chartPanel.repaint();
            }
        });
        this.add(chartPanel);
    }

    public boolean isEmpty() {
        return projectMetrics.values().stream().allMatch(Collection::isEmpty);
    }

    public static <K, V> LinkedHashMap<K, V> shiftRight(Map<K, V> map) {
        LinkedHashMap<K, V> result = new LinkedHashMap<>();

        if (map.isEmpty()) {
            return result;
        }

        List<Map.Entry<K, V>> entries = new ArrayList<>(map.entrySet());

        // Last element goes to position 1
        Map.Entry<K, V> last = entries.get(entries.size() - 1);
        result.put(last.getKey(), last.getValue());

        // All other elements move one position forward
        for (int i = 0; i < entries.size() - 1; i++) {
            Map.Entry<K, V> entry = entries.get(i);
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
