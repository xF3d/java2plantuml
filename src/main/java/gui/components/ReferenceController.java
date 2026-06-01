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

    private final JPanel buttonHolder = new JPanel();

    private final JButton toListButton = new JButton("List");
    private final Function<String[], ? extends Container> toListAction;

    private final JButton toTableButton = new JButton("Table");
    private final Function<String[], ? extends Container> toTableAction;

    private Component toRemove = null;

    private final ExecutorService background = Executors.newSingleThreadExecutor();

    public ReferenceController(String entryText, String[] references, Function<String[], ? extends Container> toListAction, Function<String[], ? extends Container> toTableAction) {
        this.entryLabel = new JLabel(entryText);
        this.references = references;
        this.toListAction = toListAction;
        this.toTableAction = toTableAction;

        toListButton.addActionListener(e -> toList());

        toTableButton.addActionListener(e -> toTable());

        this.add(buttonHolder);

        this.buttonHolder.add(entryLabel);
        this.buttonHolder.add(toListButton);
        this.buttonHolder.add(toTableButton);

        this.buttonHolder.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        toList();
    }

    public void toList() {
        CompletableFuture
            .supplyAsync(() -> toListAction.apply(references), background)
            .thenAccept(component -> SwingUtilities.invokeLater(() -> {
                updateRemoval(component);
                add(component);
                updateUI();
            }));
    }

    public void toTable() {
        CompletableFuture
            .supplyAsync(() -> toTableAction.apply(references), background)
            .thenAccept(component -> SwingUtilities.invokeLater(() -> {
                updateRemoval(component);
                add(component);
                updateUI();
            }));
    }

    private void updateRemoval(Component component) {
        if (toRemove != null) this.remove(toRemove);
        toRemove = component;
    }
}
