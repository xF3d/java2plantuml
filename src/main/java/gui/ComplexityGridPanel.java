package gui;

import translate.complexity.ComplexityMetricResult;

import javax.swing.*;
import java.awt.*;

public class ComplexityGridPanel extends JPanel {
    public static final int WIDTH = 12;
    public static final int HEIGHT = 2;

    public ComplexityGridPanel()   {
        //grid layout 6x6
        setLayout(new GridLayout(WIDTH, HEIGHT));
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

        JLabel toAdd = new JLabel("<html>" + result.getName() + " = " + valueWithColor + "</html>");
        this.add(toAdd);
    }
}
