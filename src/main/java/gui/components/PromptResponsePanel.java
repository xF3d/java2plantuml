package gui.components;

import ai.DiffPromptResponse;

import javax.swing.*;

public class PromptResponsePanel extends JPanel {
    private final DiffPromptResponse response;

    public PromptResponsePanel(DiffPromptResponse response) {
        this.response = response;

        JPanel top = new JPanel();

        var topLabel = new JLabel("Code difference");
        var topText = new JTextArea();
        topText.setEditable(false);
        topText.setText(String.join("\n", response.diffs()));

        top.add(topLabel);
        top.add(topText);



        JPanel bottom = new JPanel();

        var bottomLabel = new JLabel("Notes");
        var bottomText = new JTextArea();
        bottomText.setEditable(false);
        bottomText.setText(String.join("\n", response.notes()));

        bottomText.add(bottomLabel);
        bottom.add(bottomText);

        super.add(top);
        super.add(bottom);
    }
}
