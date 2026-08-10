package gui.components;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ReferenceController extends JPanel {
    private final JLabel entryLabel;
    protected final String[] references;

    private final ExecutorService background = Executors.newSingleThreadExecutor();

    public ReferenceController(String entryText, String[] references, Function<String[], ? extends Container> toTableAction) {
        this.entryLabel = new JLabel(entryText);
        this.references = references;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(entryLabel);
        CompletableFuture
            .supplyAsync(() -> toTableAction.apply(references), background)
            .whenComplete((component, error) -> SwingUtilities.invokeLater(() -> {
                background.shutdown();

                if (error != null) {
                    error.printStackTrace();
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    add(component);
                    revalidate();
                    repaint();
                });
            }));
    }
}
