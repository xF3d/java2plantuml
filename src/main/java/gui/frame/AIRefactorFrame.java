package gui.frame;

import translate.translator.ComplexityTranslator;

import javax.swing.*;

public class AIRefactorFrame extends JFrame {
    private final ComplexityTranslator complexityTranslator;

    public AIRefactorFrame(ComplexityTranslator translator) {
        super("AI Refactoring");
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(1000, 600);

        this.complexityTranslator = translator;

        FrameManager.addFrame(this);
    }
}
