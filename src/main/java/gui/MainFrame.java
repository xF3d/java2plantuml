package gui;

import source.DirectoryExplorer;
import source.FileHandler;
import translate.ClassDiagramConfig;
import translate.UmlTranslator;
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
    private final JTextArea outputArea; //todo make output
    private final JButton selectFolderButton, generateButton;
    private File selectedDirectory;

    public MainFrame() {
        super("Java2plantuml");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setSize(500, 500);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        selectFolderButton = new JButton("Select Source Folder");
        generateButton = new JButton("Generate UML");
        generateButton.setEnabled(false); // only enabled after folder selected

        selectFolderButton.addActionListener(this::handleFolderSelection);
        generateButton.addActionListener(this::generateUml);

        JPanel topPanel = new JPanel();
        topPanel.add(selectFolderButton);
        topPanel.add(generateButton);

        super.getContentPane().add(topPanel, BorderLayout.NORTH);
        super.getContentPane().add(new JScrollPane(outputArea), BorderLayout.CENTER);

        super.setVisible(true);
    }

    private void handleFolderSelection(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = chooser.getSelectedFile();
            generateButton.setEnabled(true);
            outputArea.setText("Selected directory: " + selectedDirectory.getAbsolutePath() + "\n");
        }
    }

    private void generateUml(ActionEvent e) {
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

                FileHandler handler = new FileHandler(umlTranslator, outputArea);

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


    }
}
