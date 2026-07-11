package gui;

import translate.complexity.ComplexityMetricResult;
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
        this(false);
    }

    public void addMetrics(ComplexityResultAggregator aggregator) {
        for (var result : aggregator) {
            addMetric(result);
        }
    }

    public void addMetric(ComplexityMetricResult result) {
        JLabel toAdd = new JLabel(result.toHtml(color));
        toAdd.setPreferredSize(new Dimension(toAdd.getPreferredSize().width, 30));
        this.add(toAdd);
    }
}
