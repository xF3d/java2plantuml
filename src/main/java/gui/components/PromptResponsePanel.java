package gui.components;

import ai.DiffPromptResponse;

import javax.swing.*;

public class PromptResponsePanel extends JScrollPane {

    public PromptResponsePanel(DiffPromptResponse response) {
        if (response.isEmpty()) {
            super.add(new JLabel("There was an error"));
            return;
        }

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

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

        main.add(center);
        main.add(bottom);

        super.add(main);
    }
}
