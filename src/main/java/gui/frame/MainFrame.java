package gui.frame;

import file.FileLogger;
import lombok.Getter;
import translate.ResolverUtils;
import translate.translator.ComplexityTranslator;
import translate.translator.GroupedTranslators;
import translate.translator.TranslatorConfig;
import translate.translator.UmlTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class MainFrame extends JFrame {
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final JPanel contentPanel = new JPanel();

    private final JPanel topPanel = new JPanel();
    private final JButton umlButton = new JButton("Generate UML");
    private final JButton complexityButton = new JButton("Calculate Complexity");
    private final JButton recomputeButton = new JButton("Recompute");

    private final GroupedTranslators groupedTranslators = new GroupedTranslators();

    private final JLabel sourceFolderLabel = new JLabel();

    @Getter
    private static File selectedDirectory;

    public MainFrame() {
        super("Java2plantuml");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setSize(1000, 600);


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
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

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
        FileLogger.LOGGER.info("Selected directory: " + selectedDirectory.getAbsolutePath());
    }

    private void generateUml(ActionEvent e) {

        backgroundExecutor.submit(() -> {
            processInit();
            try {
                UmlTranslator umlTranslator = new UmlTranslator();
                TranslatorConfig.initDefaults(umlTranslator);


                FileLogger.LOGGER.info("Generating UML\n");
                if (!umlTranslator.execute()) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Selected folder is invalid.",
                        "Invalid Folder",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                FileLogger.LOGGER.info("UML generation ended\n");

                String plantUmlOutput = umlTranslator.toPlantUml();

                // todo: check if this still works if not reimplement it
                //FileLogger.LOGGER.info("\nUML diagram saved to output.puml\n");

                new PlantUMLFrame(plantUmlOutput);

            } catch (Exception ex) {
                FileLogger.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                ex.printStackTrace();
            }

        });
    }

    private void generateComplexity(ActionEvent e) {
        backgroundExecutor.submit(() -> {
            processInit();
            try {
                ComplexityTranslator complexityTranslator = new ComplexityTranslator();
                TranslatorConfig.initDefaults(complexityTranslator);

                if (!complexityTranslator.execute()) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Selected folder is invalid.",
                        "Invalid Folder",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                contentPanel.updateUI();

                new SystemStatus(complexityTranslator);
            } catch (Exception ex) {
                FileLogger.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                ex.printStackTrace();
            }
        });

    }

    private void processInit() {
        ResolverUtils.initialize(selectedDirectory);
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
