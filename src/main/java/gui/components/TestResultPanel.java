package gui.components;

import buildapi.result.TestResult;

import javax.swing.*;

public class TestResultPanel extends JPanel {

    public TestResultPanel(String name, TestResult result) {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (!result.success()) {
            addCentered("Test of " + name + " failed with error: " + result.errorMessage());
            return;
        }

        addCentered("Test results of " + name);
        addCentered("Total tests: " + result.testsTotal());
        addCentered("Passed test: " + result.testsPassed());
        addCentered("Failed test: " + result.testsFailed());
        addCentered("Skipped test: " + result.testsSkipped());
    }

    private void addCentered(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(CENTER_ALIGNMENT);
        add(label);
    }
}
