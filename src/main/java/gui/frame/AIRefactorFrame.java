package gui.frame;

import ai.AIManager;
import ai.DiffPromptResponse;
import file.FileLogger;
import gui.GuiUtils;
import gui.components.PromptResponsePanel;
import translate.component.formatter.MemberFormatter;
import translate.dummy.DummyProject;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AIRefactorFrame extends JFrame {
    private final ComplexityTranslator complexityTranslator;
    private ExecutorService aiExecutor;

    private final JPanel cards;

    private int currentCard = 0;
    private final AtomicInteger cardSize = new AtomicInteger(0);

    private DiffPromptResponse[] responses = null;
    private boolean[] selected = null;

    private final AtomicInteger generation = new AtomicInteger(0);
    private final DummyProject dummyProject;

    public AIRefactorFrame(ComplexityTranslator translator) {
        super("AI Refactoring");
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        AIManager.init();

        this.complexityTranslator = translator;
        this.dummyProject = new DummyProject(complexityTranslator);
        this.aiExecutor = Executors.newVirtualThreadPerTaskExecutor();

        CardLayout cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        setupKeys(cards, cardLayout);
        setupMenu();

        super.add(cards);
        super.setVisible(true);
        FrameManager.addFrame(this);
    }

    private void setupKeys(JPanel cards, CardLayout layout) {
        JRootPane root = this.getRootPane();

        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
        am.put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int cs = cardSize.get();
                if (cs == 0) return;
                currentCard = (currentCard + 1) % cs;
                layout.show(cards, String.valueOf(currentCard + 1));
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous");
        am.put("previous", new AbstractAction() {
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

        menuBar.add(setupPromptMenu());
        menuBar.add(setupShowMenu());

        this.setJMenuBar(menuBar);
    }

    private JMenu setupPromptMenu() {
        JMenu promptsMenu = new JMenu("Prompts");

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

        return promptsMenu;
    }

    private JMenu setupShowMenu() {
        JMenu showMenu = new JMenu("Show Complexity");

        JMenuItem showSelected = new JMenuItem("Selected");
        showSelected.addActionListener(e -> {
            ArrayList<DiffPromptResponse> selectedResponse = new ArrayList<>();

            for (int i = 0; i < responses.length; i++) {
                if (!selected[i]) continue;

                var response = responses[i];
                if (response.isEmpty()) continue;

                selectedResponse.add(response);
            }

            dummyProject.showDiffs(selectedResponse);
        });

        JMenuItem showAll = new JMenuItem("All");
        showAll.addActionListener(e -> {
            dummyProject.showDiffs(
                    Arrays.stream(responses).filter(d -> !d.isEmpty()).toList()
            );
        });

        showMenu.add(showSelected);
        showMenu.add(showAll);

        return showMenu;
    }

    private JMenu setupApproveMenu() {
        JMenu approveMenu = new JMenu("Approve");

        JMenuItem approveSelected = new JMenuItem("Selected");

        JMenuItem approveAll = new JMenuItem("All");

        approveMenu.add(approveSelected);
        approveMenu.add(approveAll);

        return approveMenu;
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

        int gen = generation.get();
        for (var classEntry : methods.entrySet()) {
            String fqn = classEntry.getKey();
            Map<String, ComplexityMethod> methodMap = classEntry.getValue();

            for (var methodEntry : methodMap.values()) {

                this.aiExecutor.submit(() -> {

                    var cclass = complexityTranslator.getClassResultMap().get(fqn);

                    DiffPromptResponse diff = aiManager.fixMethod(cclass, methodEntry);
                    if (gen != generation.get()) {
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {
                        if (gen != generation.get()) {
                            return;
                        }

                        this.addCard(diff);
                    });
                });
            }
        }
    }

    public void export(ActionEvent e) {
        if (responses == null) FileLogger.LOGGER.warning("Empty responses");

        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }


        File exportFile = chooser.getSelectedFile();
        String content = DiffPromptResponse.toFileContents(List.of(
                Arrays.copyOfRange(responses, 0, cardSize.get())
        ));
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
            var response = diffs.get(i);
            responses[i] = response;
            addCard(response);
        }
    }

    public void reset(ActionEvent ignored) {
        // Remove all cards
        cards.removeAll();

        // Refresh the UI
        cards.revalidate();
        cards.repaint();

        responses = null;
        selected = null;

        generation.incrementAndGet();

        currentCard = 0;
        aiExecutor.shutdownNow();
        aiExecutor = Executors.newVirtualThreadPerTaskExecutor();
        cardSize.set(0);
    }

    public void addCard(DiffPromptResponse diff) {
        int current = cardSize.getAndIncrement();
        String pageNString = String.valueOf(current + 1);
        JPanel panel = new JPanel();
        GuiUtils.setVerticalLayout(panel);

        String name = diff.origin().replace(MemberFormatter.PACKAGE_DELIMITER, ".");
        panel.add(new JLabel("Page " + pageNString + "/" + responses.length));
        panel.add(new JLabel(name));
        panel.add(new PromptResponsePanel(diff));

        JCheckBox checkBox = new JCheckBox("Approve");
        checkBox.addActionListener(e -> selected[current] = checkBox.isSelected());

        panel.add(checkBox);

        cards.add(panel, pageNString);
        cards.revalidate();
        cards.repaint();
    }
}
