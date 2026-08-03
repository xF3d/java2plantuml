package gui.components;

import ai.DiffPromptResponse;
import gui.GuiUtils;

import javax.swing.*;

public class PromptResponsePanel extends JScrollPane {

    public PromptResponsePanel(DiffPromptResponse response) {
        if (response.isEmpty()) {
            setViewportView(new JLabel("There was an error"));
            return;
        }

        JPanel main = new JPanel();
        GuiUtils.setVerticalLayout(main);

        JPanel center = new JPanel();
        GuiUtils.setVerticalLayout(center);

        var centerLabel = new JLabel("New methods");
        var centerText = new JTextArea();
        centerText.setEditable(false);
        centerText.setFocusable(false);
        centerText.setText(String.join("\n", response.newMethods()));

        center.add(centerLabel);
        center.add(centerText);


        JPanel bottom = new JPanel();
        GuiUtils.setVerticalLayout(bottom);

        var bottomLabel = new JLabel("Notes");
        var bottomText = new JTextArea();
        bottomText.setEditable(false);
        bottomText.setFocusable(false);
        bottomText.setText(String.join("\n", response.notes()));

        bottom.add(bottomLabel);
        bottom.add(bottomText);

        main.add(center);
        main.add(bottom);

        setViewportView(main);
    }

}
