package gui.frame;

import file.FileLogger;
import lombok.Getter;
import translate.ResolverUtils;
import translate.translator.ComplexityTranslator;
import file.Config;
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
    private final JButton aiButton = new JButton("AI Refactor Issues");
    private final JButton recalculateButton = new JButton("Recalculate");

    private final JLabel sourceFolderLabel = new JLabel();

    private ComplexityTranslator complexityTranslator = new ComplexityTranslator();

    @Getter
    private static File selectedDirectory;

    public MainFrame() {
        super("Java2plantuml");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setSize(1000, 600);

        JButton selectFolderButton = new JButton("Select Source Folder");
        selectFolderButton.addActionListener(this::handleFolderSelection);

        umlButton.addActionListener(this::generateUml);
        complexityButton.addActionListener(this::generateComplexity);
        aiButton.addActionListener(this::generateRefactor);
        recalculateButton.addActionListener(this::recalculate);

        this.setButtons(false);

        topPanel.add(selectFolderButton);
        topPanel.add(umlButton);
        topPanel.add(complexityButton);
        topPanel.add(aiButton);
        topPanel.add(recalculateButton);

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
        this.setButtons(true);
        sourceFolderLabel.setText("Selected directory: " + selectedDirectory.getAbsolutePath() + "\n");
    }

    private void setButtons(boolean state) {
        umlButton.setEnabled(state);
        complexityButton.setEnabled(state);
        recalculateButton.setEnabled(state);
        aiButton.setEnabled(state);
    }

    private void generateUml(ActionEvent ignored) {
        backgroundExecutor.submit(() -> {
            processInit();
            try {
                UmlTranslator umlTranslator = new UmlTranslator();
                Config.getInstance().updateTranslator(umlTranslator);

                FileLogger.LOGGER.info("Generating UML\n");
                if (!umlTranslator.execute(selectedDirectory)) {
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

                if (!complexityTranslator.execute(selectedDirectory)) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Selected folder is invalid.",
                            "Invalid Folder",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                new PlantUMLFrame(plantUmlOutput, complexityTranslator);

            } catch (Exception ex) {
                FileLogger.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                ex.printStackTrace();
            }

        });
    }

    private void generateComplexity(ActionEvent ignored) {
        backgroundExecutor.submit(() -> {
            processInit();
            try {
                Config.getInstance().updateTranslator(complexityTranslator);

                if (!complexityTranslator.execute(selectedDirectory)) {
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

    private void generateRefactor(ActionEvent ingored) {
        backgroundExecutor.submit(() -> {
            processInit();
            try {
                Config.getInstance().updateTranslator(complexityTranslator);

                if (!complexityTranslator.execute(selectedDirectory)) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Selected folder is invalid.",
                            "Invalid Folder",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                contentPanel.updateUI();

                new AIRefactorFrame(complexityTranslator);
                
            } catch (Exception ex) {
                FileLogger.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                ex.printStackTrace();
            }
        });
    }

    private void recalculate(ActionEvent actionEvent) {
        FrameManager.closeAll();
        complexityTranslator = new ComplexityTranslator();
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
