package gui.frame;

import ai.AIManager;
import gui.components.PromptResponsePanel;
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AIRefactorFrame extends JFrame {
    private final ComplexityTranslator complexityTranslator;
    private int currentCard = 0;
    private final int cardSize;

    public AIRefactorFrame(ComplexityTranslator translator) {
        super("AI Refactoring");
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        AIManager.init();

        this.complexityTranslator = translator;

        CardLayout layout = new CardLayout();
        JPanel cards = new JPanel(layout);
        var aiManager = AIManager.getInstance();

        int pageNumber = 1;
        var methods = complexityTranslator.getProblematicMethods();

        for (var classEntry : methods.entrySet()) {
            String fqn = classEntry.getKey();
            for (var methodEntry : classEntry.getValue().entrySet()) {
                String signature = methodEntry.getKey();
                var method = methodEntry.getValue();

                var cclass = complexityTranslator.getClassResultMap().get(fqn);
                var diff = aiManager.chat(cclass, method);

                String pageNString = String.valueOf(pageNumber++);
                JPanel panel = new JPanel();

                panel.add(new JLabel("Page " + pageNString));
                panel.add(new JLabel(fqn.replace(MemberFormatter.PACKAGE_DELIMITER, ".") + "#" + signature));
                panel.add(new PromptResponsePanel(diff));
                cards.add(panel, pageNString);
            }
        }

        // Right arrow
        cards.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("RIGHT"), "next");

        cardSize = pageNumber - 1;
        cards.getActionMap().put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentCard = (currentCard + 1) % cardSize;
                layout.show(cards, String.valueOf(currentCard));
            }
        });

        // Left arrow
        cards.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("LEFT"), "previous");

        cards.getActionMap().put("previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentCard = (currentCard - 1 + cardSize) % cardSize;
                layout.show(cards, String.valueOf(currentCard));
            }
        });

        super.add(cards);
        super.setVisible(true);
        FrameManager.addFrame(this);
    }

}
