package gui.components;

import ai.reponse.ApplicableDiff;
import ai.reponse.DiffClassResponse;
import ai.reponse.DiffMethodResponse;
import gui.GuiUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PromptResponsePanel extends JScrollPane {

    public PromptResponsePanel(ApplicableDiff response) {
        if (response.isEmpty()) {
            setViewportView(new JLabel("There was an error"));
            return;
        }

        if (response instanceof DiffMethodResponse diffMethodResponse) {
            setupMethod(diffMethodResponse);
        } else if (response instanceof DiffClassResponse diffClassResponse) {
            setupClass(diffClassResponse);
        } else {
            throw new RuntimeException("Invalid Applicable Diff detected");
        }
    }

    private void setupMethod(DiffMethodResponse response) {
        JPanel main = new JPanel();
        GuiUtils.setVerticalLayout(main);

        JPanel newMethodsArea = setupLabelArea("New Methods", String.join("\n", response.newMethods()));
        JPanel notesArea = setupLabelArea("Notes", String.join("\n", response.notes()));

        main.add(newMethodsArea);
        main.add(notesArea);

        setViewportView(main);
    }

    private void setupClass(DiffClassResponse response) {
        JPanel main = new JPanel();
        GuiUtils.setVerticalLayout(main);

        JPanel addedImportsArea = setupLabelArea("Added Imports", String.join("\n", response.addedImports()));
        JPanel changesArea = setupLabelArea("Changes", String.join("\n", response.changes()));

        List<JPanel> addedClassesAreas = new ArrayList<>();
        List<String> added = response.added();
        for (int i = 0; i < added.size(); i++) {
            String code = added.get(i);
            addedClassesAreas.add(
                    setupLabelArea("Added Class " + (i + 1), code)
            );
        }

        JPanel notesArea = setupLabelArea("Notes", String.join("\n", response.notes()));

        main.add(addedImportsArea);
        main.add(changesArea);
        addedClassesAreas.forEach(main::add);
        main.add(notesArea);

        setViewportView(main);
    }

    private JPanel setupLabelArea(String title, String contents) {
        if (contents.trim().isEmpty()) return new JPanel();

        var panel = new JPanel();
        GuiUtils.setVerticalLayout(panel);

        var label = new JLabel(title);
        var area = new JTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setText(contents);

        panel.add(label);
        panel.add(area);

        return panel;
    }
}
