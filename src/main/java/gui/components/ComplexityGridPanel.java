package gui.components;

import gui.RatioGridLayout;
import translate.complexity.result.BoundedResult;
import translate.complexity.result.ComplexityMetricResult;
import translate.complexity.ComplexityResultAggregator;

import javax.swing.*;
import java.awt.*;

public class ComplexityGridPanel extends JPanel {
    private final boolean color;

    public ComplexityGridPanel(boolean color) {
        this.color = color;
        setLayout(new RatioGridLayout(5, 5, new RatioGridLayout.Ratio(6, 1)));
    }

    public ComplexityGridPanel() {
        this(true);
    }

    public void addMetrics(ComplexityResultAggregator aggregator) {
        for (var result : aggregator.getRawMetricData()) {
            addMetric(result);
        }
    }

    public void addMetrics(Iterable<BoundedResult> iterable) {
        for (var result : iterable) {
            addMetric(result);
        }
    }

    public void addMetric(BoundedResult result) {
        JTextPane toAdd = new JTextPane();
        toAdd.setContentType("text/html");
        toAdd.setText(result.toHtml(color));
        toAdd.setEditable(false);
        toAdd.setBackground(null);
        toAdd.setBorder(null);
        toAdd.setPreferredSize(new Dimension(toAdd.getPreferredSize().width, 30));
        this.add(toAdd);
    }
}
