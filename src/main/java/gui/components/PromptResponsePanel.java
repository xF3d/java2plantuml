package gui.components;

import ai.DiffPromptResponse;

import javax.swing.*;

public class PromptResponsePanel extends JScrollPane {
    private final DiffPromptResponse response;

    public PromptResponsePanel(DiffPromptResponse response) {
        this.response = response;
        if (response == DiffPromptResponse.EMPTY) {
            super.add(new JLabel("There was an error"));
            return;
        }

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel center = new JPanel();

        var centerLabel = new JLabel("New methods");
        var centerText = new JTextArea();
        centerText.setEditable(false);
        centerText.setText(String.join("\n", response.newMethods()));

        center.add(centerLabel);
        center.add(centerText);


        JPanel bottom = new JPanel();

        var bottomLabel = new JLabel("Notes");
        var bottomText = new JTextArea();
        bottomText.setEditable(false);
        bottomText.setText(String.join("\n", response.notes()));

        bottomText.add(bottomLabel);
        bottom.add(bottomText);

        super.add(center);
        super.add(bottom);
    }
}
