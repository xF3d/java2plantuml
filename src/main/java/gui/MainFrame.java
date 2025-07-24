package gui;

import source.DirectoryExplorer;
import source.FileHandler;
import translate.ClassDiagramConfig;
import translate.translator.ComplexityTranslator;
import translate.translator.UmlTranslator;
import visitors.ClassVisitor;
import visitors.EnumVisitor;
import visitors.InterfaceVisitor;
import visitors.RecordVisitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;

public class MainFrame extends JFrame {
    private final JButton umlButton = new JButton("Generate UML");
    private final JButton complexityButton = new JButton("Calculate Complexity");
    private final JLabel sourceFolderLabel = new JLabel();
    private File selectedDirectory;

    private Component toRemove = null;

    public MainFrame() {
        super("Java2plantuml");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setSize(500, 500);

        JButton selectFolderButton = new JButton("Select Source Folder");
        selectFolderButton.addActionListener(this::handleFolderSelection);

        umlButton.setEnabled(false); // only enabled after folder selected
        umlButton.addActionListener(this::generateUml);

        complexityButton.setEnabled(false);
        complexityButton.addActionListener(this::generateComplexity);

        JPanel topPanel = new JPanel();
        topPanel.add(selectFolderButton);
        topPanel.add(umlButton);
        topPanel.add(complexityButton);

        super.getContentPane().add(topPanel, BorderLayout.NORTH);
        super.getContentPane().add(sourceFolderLabel, BorderLayout.CENTER);

        super.setVisible(true);
    }

    private void handleFolderSelection(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = chooser.getSelectedFile();
            umlButton.setEnabled(true);
            complexityButton.setEnabled(true);
            sourceFolderLabel.setText("Selected directory: " + selectedDirectory.getAbsolutePath() + "\n");
        }
    }

    private void generateUml(ActionEvent e) {
        if (toRemove != null) {
            super.getContentPane().remove(toRemove);
        }

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        super.getContentPane().add(outputArea, BorderLayout.CENTER);

        outputArea.setText("");
        outputArea.append("Generating UML...\n");

        new Thread(() -> {
            try {
                UmlTranslator umlTranslator = new UmlTranslator(outputArea);
                UmlTranslator.config = new ClassDiagramConfig.Builder()
                        .withVisitor(new ClassVisitor(umlTranslator))
                        .withVisitor(new InterfaceVisitor(umlTranslator))
                        .withVisitor(new EnumVisitor(umlTranslator))
                        .withVisitor(new RecordVisitor(umlTranslator))
                        .setShowMethods(true)
                        .setShowAttributes(true)
                        .setShowColoredAccessSpecifiers(false)
                        .build();

                FileHandler handler = new FileHandler(umlTranslator);

                if (selectedDirectory != null && selectedDirectory.exists()) {
                    new DirectoryExplorer(handler).explore(selectedDirectory);
                } else {
                    outputArea.append("Selected folder is invalid.\n");
                    return;
                }

                String plantUmlOutput = umlTranslator.toPlantUml();

                // Optional: write to file too
                File f = new File("output.puml");
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write(plantUmlOutput.getBytes());
                }

                outputArea.append("\nUML diagram saved to output.puml\n");

            } catch (Exception ex) {
                outputArea.append("An error occurred: " + ex.getMessage() + "\n");
                ex.printStackTrace();
            }
        }).start();

        toRemove = outputArea;
    }

    private void generateComplexity(ActionEvent e) {
        if (toRemove != null) {
            super.getContentPane().remove(toRemove);
        }

        JPanel panel = new JPanel();
        ComplexityGridPanel mainMetricPanel = new ComplexityGridPanel();

        panel.add(mainMetricPanel, BorderLayout.NORTH);

        new Thread(() -> {
            try {
                ComplexityTranslator umlTranslator = new ComplexityTranslator(mainMetricPanel);
                ComplexityTranslator.config = new ClassDiagramConfig.Builder()
                        .withVisitor(new ClassVisitor(umlTranslator))
                        .withVisitor(new InterfaceVisitor(umlTranslator))
                        .withVisitor(new EnumVisitor(umlTranslator))
                        .withVisitor(new RecordVisitor(umlTranslator))
                        .setShowMethods(true)
                        .setShowAttributes(true)
                        .setShowColoredAccessSpecifiers(false)
                        .build();

                FileHandler handler = new FileHandler(umlTranslator);

                if (selectedDirectory != null && selectedDirectory.exists()) {
                    new DirectoryExplorer(handler).explore(selectedDirectory);
                } else {
                    mainMetricPanel.removeAll();
                    mainMetricPanel.add(new JLabel("Selected folder is invalid."));
                    return;
                }

                JList<String> complexityList = umlTranslator.toComplexityList();
                panel.add(complexityList, BorderLayout.CENTER);

                // Optional: write to file too
                /*
                File f = new File("output.puml");
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write(plantUmlOutput.getBytes());
                }

                outputArea.append("\nUML diagram saved to output.puml\n");*/

            } catch (Exception ex) {
                mainMetricPanel.removeAll();
                mainMetricPanel.add(new JLabel("An error occurred: " + ex.getMessage()));
                ex.printStackTrace();
            }
        }).start();
    }
}
