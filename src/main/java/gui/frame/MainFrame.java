package gui.frame;

import lombok.Getter;
import source.DirectoryExplorer;
import source.FileHandler;
import translate.ClassDiagramConfig;
import translate.ResolverUtils;
import translate.translator.ComplexityTranslator;
import translate.translator.TranslatorConfig;
import translate.translator.UmlTranslator;
import visitors.AnnotationVisitor;
import visitors.ClassVisitor;
import visitors.EnumVisitor;
import visitors.InterfaceVisitor;
import visitors.RecordVisitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainFrame extends JFrame {
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final JPanel contentPanel = new JPanel();

    private final JPanel topPanel = new JPanel();
    private final JButton umlButton = new JButton("Generate UML");
    private final JButton complexityButton = new JButton("Calculate Complexity");

    private final JLabel sourceFolderLabel = new JLabel();

    private final JPanel bottomPanel = new JPanel();

    @Getter
    private static File selectedDirectory;

    private Component toRemove = null;

    public MainFrame() {
        super("Java2plantuml");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setSize(1000, 600);

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));

        JButton selectFolderButton = new JButton("Select Source Folder");
        selectFolderButton.addActionListener(this::handleFolderSelection);

        umlButton.setEnabled(false);
        umlButton.addActionListener(this::generateUml);

        complexityButton.setEnabled(false);
        complexityButton.addActionListener(this::generateComplexity);

        topPanel.add(selectFolderButton);
        topPanel.add(umlButton);
        topPanel.add(complexityButton);

        sourceFolderLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        sourceFolderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sourceFolderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        contentPanel.add(topPanel);
        contentPanel.add(sourceFolderLabel);
        contentPanel.add(bottomPanel);

        bottomPanel.setLayout(new GridLayout(1,1));

        contentPanel.addComponentListener(this.componentResized());

        super.add(contentPanel);

        super.setVisible(true);
    }

    private void handleFolderSelection(ActionEvent e) {
        FrameManager.closeAll();
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        selectedDirectory = chooser.getSelectedFile();
        umlButton.setEnabled(true);
        complexityButton.setEnabled(true);
        sourceFolderLabel.setText("Selected directory: " + selectedDirectory.getAbsolutePath() + "\n");
    }

    private void generateUml(ActionEvent e) {

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane jScrollPane = new JScrollPane(outputArea);

        outputArea.setText("");
        outputArea.append("Generating UML...\n");

        backgroundExecutor.submit(() -> {
            processInit();
            bottomPanel.add(jScrollPane);
            try {
                UmlTranslator umlTranslator = new UmlTranslator(outputArea, contentPanel::updateUI);
                TranslatorConfig.config =
                    new ClassDiagramConfig.Builder()
                        .withVisitor(new ClassVisitor(umlTranslator))
                        .withVisitor(new InterfaceVisitor(umlTranslator))
                        .withVisitor(new EnumVisitor(umlTranslator))
                        .withVisitor(new RecordVisitor(umlTranslator))
                        .withVisitor(new AnnotationVisitor(umlTranslator))
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
                contentPanel.updateUI();

                String plantUmlOutput = umlTranslator.toPlantUml();

                outputArea.append("\nUML diagram saved to output.puml\n");

                new PlantUMLFrame(plantUmlOutput);

            } catch (Exception ex) {
                outputArea.append("An error occurred: " + ex.getMessage() + "\n");
                ex.printStackTrace();
            }
            toRemove = jScrollPane;

        });
    }

    private void generateComplexity(ActionEvent e) {
        backgroundExecutor.submit(() -> {
            processInit();
            try {
                ComplexityTranslator complexityTranslator = new ComplexityTranslator();
                TranslatorConfig.config =
                    new ClassDiagramConfig.Builder()
                        .withVisitor(new ClassVisitor(complexityTranslator))
                        .withVisitor(new InterfaceVisitor(complexityTranslator))
                        .withVisitor(new EnumVisitor(complexityTranslator))
                        .withVisitor(new RecordVisitor(complexityTranslator))
                        .withVisitor(new AnnotationVisitor(complexityTranslator))
                        .setShowMethods(true)
                        .setShowAttributes(true)
                        .setShowColoredAccessSpecifiers(false)
                        .build();

                FileHandler handler = new FileHandler(complexityTranslator);

                if (selectedDirectory != null && selectedDirectory.exists()) {
                    new DirectoryExplorer(handler).explore(selectedDirectory);
                } else {
                    JLabel errorLabel = new JLabel("Selected folder is invalid.");
                    bottomPanel.add(errorLabel);
                    toRemove = errorLabel;
                    return;
                }

                JScrollPane jscroll = getJScrollPane(complexityTranslator);
                bottomPanel.add(jscroll);
                toRemove = jscroll;
                contentPanel.updateUI();

                new SystemStatus(complexityTranslator);
            } catch (Exception ex) {
                JLabel errorLabel = new JLabel("An error occurred: " + ex.getMessage());
                bottomPanel.add(errorLabel);
                toRemove = errorLabel;
                ex.printStackTrace();
            }
        });

    }

    private void processInit() {
        ResolverUtils.initialize(selectedDirectory);
        if (toRemove != null) {
            bottomPanel.remove(toRemove);
        }
    }

    private static JScrollPane getJScrollPane(ComplexityTranslator umlTranslator) {
        JList<String> complexityList = umlTranslator.getClassJList();
        complexityList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) { // double-click
                    return;
                }

                int index = complexityList.locationToIndex(e.getPoint());
                if (index == -1) {
                    return;
                }

                String selectedItem = complexityList.getModel().getElementAt(index);
                new ClassFrame(selectedItem, umlTranslator);
            }
        });

        return new JScrollPane(complexityList);
    }

    public ComponentAdapter componentResized() {
        return new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                topPanel.setSize(new Dimension(Integer.MAX_VALUE, 50));
                sourceFolderLabel.setSize(new Dimension(Integer.MAX_VALUE, 50));
                contentPanel.updateUI();
            }
        };
    }
}
