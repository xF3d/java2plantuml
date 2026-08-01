package gui.frame;

import ai.AIManager;
import ai.DiffPromptResponse;
import file.FileLogger;
import gui.components.PromptResponsePanel;
import translate.component.formatter.MemberFormatter;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AIRefactorFrame extends JFrame {
    private final ComplexityTranslator complexityTranslator;
    private ExecutorService aiExecutor;

    private final CardLayout cardLayout;
    private final JPanel cards;

    private int currentCard = 0;
    private final AtomicInteger cardSize = new AtomicInteger(0);

    private DiffPromptResponse[] responses = null;
    private boolean[] selected = null;

    private final AtomicInteger generation = new AtomicInteger(0);

    // TODO: clonare progetto e mostrare differenze
    public AIRefactorFrame(ComplexityTranslator translator) {
        super("AI Refactoring");
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        AIManager.init();

        this.complexityTranslator = translator;
        this.aiExecutor = Executors.newVirtualThreadPerTaskExecutor();

        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        setupKeys(cards, cardLayout);
        setupMenu();

        super.add(cards);
        super.setVisible(true);
        FrameManager.addFrame(this);
    }

    private void setupKeys(JPanel cards, CardLayout layout) {
        // Right arrow
        cards.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("RIGHT"), "next");

        cards.getActionMap().put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int cs = cardSize.get();
                if (cs == 0) return;
                currentCard = (currentCard + 1) % cs;
                layout.show(cards, String.valueOf(currentCard + 1));
            }
        });

        // Left arrow
        cards.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("LEFT"), "previous");

        cards.getActionMap().put("previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int cs = cardSize.get();
                if (cs == 0) return;
                currentCard = (currentCard - 1 + cs) % cs;
                layout.show(cards, String.valueOf(currentCard + 1));
            }
        });
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu promptsMenu = new JMenu("Prompts");
        menuBar.add(promptsMenu);

        JMenuItem startPrompt = new JMenuItem("Start");
        startPrompt.addActionListener(this::start);

        JMenuItem exportPrompt = new JMenuItem("Export");
        exportPrompt.addActionListener(this::export);

        JMenuItem importPrompt = new JMenuItem("Import");
        importPrompt.addActionListener(this::importAction);

        JMenuItem resetPrompt = new JMenuItem("Reset");
        resetPrompt.addActionListener(this::reset);

        promptsMenu.add(startPrompt);
        promptsMenu.add(exportPrompt);
        promptsMenu.add(importPrompt);
        promptsMenu.add(resetPrompt);

        this.setJMenuBar(menuBar);
    }

    public boolean hasCompleted() {
        return responses != null && selected != null && responses.length == cardSize.get();
    }

    public void start(ActionEvent ignored) {
        FileLogger.LOGGER.info("Start AI evaluation");
        var aiManager = AIManager.getInstance();

        var methods = complexityTranslator.getProblematicMethods();

        int totalSize = 0;
        for (var methodValues : methods.values()) {
            totalSize += methodValues.size();
        }

        final int finalTotalSize = totalSize;
        responses = new DiffPromptResponse[finalTotalSize];
        selected = new boolean[finalTotalSize];

        int x = 0;
        int gen = generation.get();
        for (var classEntry : methods.entrySet()) {
            String fqn = classEntry.getKey();
            Map<String, ComplexityMethod> methodMap = classEntry.getValue();

            for (var methodEntry : methodMap.entrySet()) {
                if (x++ > 3) break;

                this.aiExecutor.submit(() -> {
                    String signature = methodEntry.getKey();
                    var method = methodEntry.getValue();

                    var cclass = complexityTranslator.getClassResultMap().get(fqn);

                    DiffPromptResponse diff = aiManager.fixMethod(cclass, method);
                    if (gen != generation.get()) {
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {
                        if (gen != generation.get()) {
                            return;
                        }

                        this.addCard(fqn.replace(MemberFormatter.PACKAGE_DELIMITER, ".") + "#" + signature, diff);
                    });
                });
            }

            if (x > 3) break;
        }
    }

    public void export(ActionEvent e) {
        if (!hasCompleted()) return;


        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File exportFile = chooser.getSelectedFile();
        String content = DiffPromptResponse.toFileContents(List.of(responses));
        try {
            Files.writeString(exportFile.toPath(), content);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    null,
                    ex.getMessage(),
                    "Export error",
                    JOptionPane.ERROR_MESSAGE
            );
            FileLogger.LOGGER.warning(ex.getMessage());
        }
    }

    public void importAction(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File importFile = chooser.getSelectedFile();

        String contents;
        try {
            contents = Files.readString(importFile.toPath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    null,
                    ex.getMessage(),
                    "Import error",
                    JOptionPane.ERROR_MESSAGE
            );
            FileLogger.LOGGER.warning(ex.getMessage());
            return;
        }

        List<DiffPromptResponse> diffs = DiffPromptResponse.fromFileContents(contents);
        if (diffs.isEmpty() || diffs.stream().allMatch(DiffPromptResponse::isEmpty)) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not read meaningful data",
                    "Import error",
                    JOptionPane.ERROR_MESSAGE
            );
            FileLogger.LOGGER.warning("Could not read meaningful data");
            return;
        }

        reset(null);

        int size = diffs.size();
        responses = new DiffPromptResponse[size];
        selected = new boolean[size];
        for (int i = 0; i < size; i++) {
            responses[i] = diffs.get(i);
            // TODO: add "name" to DiffPromptResponse
            addCard("unk", responses[i]);
        }

    }

    public void reset(ActionEvent ignored) {
        responses = null;
        selected = null;

        generation.incrementAndGet();
        // Remove all cards
        cards.removeAll();

        // Refresh the UI
        cards.revalidate();
        cards.repaint();

        currentCard = 0;
        aiExecutor.shutdownNow();
        aiExecutor = Executors.newVirtualThreadPerTaskExecutor();
        cardSize.set(0);
    }

    public void addCard(String name, DiffPromptResponse diff) {
        String pageNString = String.valueOf(cardSize.getAndIncrement() + 1);
        JPanel panel = new JPanel();

        panel.add(new JLabel("Page " + pageNString + "/" + responses.length));
        panel.add(new JLabel(name));
        panel.add(new PromptResponsePanel(diff));
        cards.add(panel, pageNString);
        cards.revalidate();
        cards.repaint();
    }
}
