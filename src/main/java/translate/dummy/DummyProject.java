package translate.dummy;

import ai.DiffPromptResponse;
import file.FolderSync;
import gui.components.SystemChartPanel;
import gui.frame.FrameManager;
import gui.frame.MainFrame;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class DummyProject {
    private final Path dummyLocation;
    private final CompletableFuture<Void> copyingTask;
    private ComplexityTranslator dummyTranslator = null;
    private final ComplexityTranslator originalTranslator;

    public DummyProject(Path dummyLocation, ComplexityTranslator originalTranslator) {
        this.dummyLocation = dummyLocation;
        this.originalTranslator = originalTranslator;
        File mainProject = MainFrame.getSelectedDirectory();

        try (ExecutorService exe = Executors.newVirtualThreadPerTaskExecutor()) {
            copyingTask = CompletableFuture.runAsync(
                    () -> {
                        try {
                            FolderSync.sync(mainProject.toPath(), dummyLocation);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, exe
            );
        }
    }

    public DummyProject(ComplexityTranslator originalTranslator) {
        this(getTempProject(), originalTranslator);
    }

    public void showDiffs(List<DiffPromptResponse> diffs) {
        setTranslator();

        ArrayList<DiffApplier> appliers = new ArrayList<>();

        for (var diff : diffs) {
            String[] split = diff.origin().split(Pattern.quote("#"));
            if (split.length != 2) throw new RuntimeException("Invalid diff " + diff);

            String fqn = split[0];
            String signature = split[1];

            var originalMethod = originalTranslator.getMethod(fqn, signature).getMethod();
            appliers.add(new DiffApplier(originalMethod.clone(), diff, dummyTranslator));
        }

        appliers.forEach(DiffApplier::apply);

        var tempTranslator = new ComplexityTranslator();
        tempTranslator.execute(dummyLocation.toFile());

        JFrame comparison = new JFrame();
        SystemChartPanel panel = new SystemChartPanel();


        comparison.add(panel);
        comparison.setVisible(true);
        comparison.setExtendedState(comparison.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        FrameManager.addFrame(comparison);

        appliers.forEach(DiffApplier::revert);
    }

    public void applyDiffs(List<DiffPromptResponse> diffs) {
        setTranslator();

        for (var diff : diffs) {
            String[] split = diff.origin().split(Pattern.quote("#"));
            if (split.length != 2) throw new RuntimeException("Invalid diff " + diff);

            String fqn = split[0];
            String signature = split[1];

            var originalMethod = originalTranslator.getMethod(fqn, signature).getMethod();
            new DiffApplier(originalMethod, diff).apply();
        }
    }

    private void setTranslator() {
        if (dummyTranslator != null) return;

        try {
            copyingTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        this.dummyTranslator = new ComplexityTranslator();
        this.dummyTranslator.execute(dummyLocation.toFile());
    }


    public static Path getTempProject() {
        Path folder = Paths.get(
                System.getProperty("user.home"),
                "JOverview",
                "tempproject"
        );

        try {
            Files.createDirectories(folder); // creates it if missing
        } catch (IOException e) {
            e.printStackTrace();
        }

        return folder;
    }
}
