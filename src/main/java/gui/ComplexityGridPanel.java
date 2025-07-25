package gui;

import translate.complexity.ComplexityMetricResult;

import javax.swing.*;
import java.awt.*;

public class ComplexityGridPanel extends JPanel {

    public ComplexityGridPanel()   {
        setLayout(new RatioGridLayout(5, 5, new RatioGridLayout.Ratio(6, 1)));
    }

    public void addMetrics(ComplexityMetricResult... manyResults) {
        for (var result : manyResults) {
            addMetric(result);
        }
    }

    public void addMetric(ComplexityMetricResult result) {
        String valueWithColor;
        if (result.getMin() < result.getValue() && result.getValue() < result.getMax()) {
            valueWithColor = "<font color='green'> " + result.getValue() + "</font>";
        } else {
            valueWithColor = "<font color='red'> " + result.getValue() + "</font>";
        }

        JLabel toAdd = new JLabel("<html><div style='white-space: nowrap;'>" + result.getName() + " = " + valueWithColor + "</div></html>");
        toAdd.setPreferredSize(new Dimension(toAdd.getPreferredSize().width, 30));
        this.add(toAdd);
    }
}
