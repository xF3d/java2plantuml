package translate.dummy;

import ai.reponse.ApplicableDiff;
import buildapi.runner.BuildRunner;
import file.FolderSync;
import gui.frame.CompareFrame;
import gui.frame.MainFrame;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DummyProject {
    private final Path dummyLocation;
    private CompletableFuture<Void> copyingTask;
    private ComplexityTranslator dummyTranslator = null;
    private final ComplexityTranslator originalTranslator;
    private final BuildRunner buildRunner;

    public DummyProject(Path dummyLocation, ComplexityTranslator originalTranslator) {
        this.dummyLocation = dummyLocation;
        this.originalTranslator = originalTranslator;

        File mainProject = MainFrame.getSelectedDirectory();
        this.buildRunner = BuildRunner.of(mainProject.toPath());
        recopy();
    }

    public DummyProject(ComplexityTranslator originalTranslator) {
        this(getTempProject(), originalTranslator);
    }

    synchronized public void showDiffs(List<? extends ApplicableDiff> diffs) {
        init();

        for (var diff : diffs) {
            try {
                diff.apply(originalTranslator, diff.getPath(dummyTranslator));
            } catch (Exception ignored) {
                // method is not present anymore
            }
        }

        var tempTranslator = new ComplexityTranslator();
        tempTranslator.execute(dummyLocation.toFile());

        new CompareFrame(originalTranslator, tempTranslator);

        /*
        var dummyResult = CompletableFuture.supplyAsync( () -> buildRunner.test(dummyLocation), exe);
        var originalResult = CompletableFuture.supplyAsync( () -> buildRunner.test(MainFrame.getSelectedDirectory().toPath()), exe);

        dummyResult.thenCombineAsync(originalResult, (dummy, original) -> {
            SwingUtilities.invokeLater(
                    () -> new CompareFrame(originalTranslator, tempTranslator, original, dummy)
            );
            return null;
        }, exe).whenComplete((r, e) -> {
            recopy();
            exe.shutdown();
            e.printStackTrace();
        });*/

    }

    public void applyDiffs(List<? extends ApplicableDiff> diffs) {
        init();

        for (var diff : diffs) {
            try {
                diff.apply(originalTranslator, diff.getPath(originalTranslator));
            } catch (Exception e) {
                // method is not present anymore
            }
        }
    }

    private void init() {
        if (dummyTranslator != null) return;

        try {
            copyingTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        this.dummyTranslator = new ComplexityTranslator();
        this.dummyTranslator.execute(dummyLocation.toFile());
    }

    private void recopy() {
        File mainProject = MainFrame.getSelectedDirectory();

        dummyTranslator = null;
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
