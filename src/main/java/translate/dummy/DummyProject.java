package translate.dummy;

import ai.DiffPromptResponse;
import buildapi.result.TestResult;
import buildapi.runner.BuildRunner;
import file.FolderSync;
import gui.frame.CompareFrame;
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

public class DummyProject {
    private final Path dummyLocation;
    private final CompletableFuture<Void> copyingTask;
    private ComplexityTranslator dummyTranslator = null;
    private final ComplexityTranslator originalTranslator;
    private final BuildRunner buildRunner;

    public DummyProject(Path dummyLocation, ComplexityTranslator originalTranslator) {
        this.dummyLocation = dummyLocation;
        this.originalTranslator = originalTranslator;
        File mainProject = MainFrame.getSelectedDirectory();
        this.buildRunner = BuildRunner.of(mainProject.toPath());

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

    synchronized public void showDiffs(List<DiffPromptResponse> diffs) {
        init();

        ArrayList<DiffApplier> appliers = new ArrayList<>();

        for (var diff : diffs) {
            var originalMethod = originalTranslator.getMethod(diff.clazz(), diff.method()).getMethod();
            appliers.add(new DiffApplier(originalMethod, diff, dummyTranslator));
        }

        appliers.forEach(DiffApplier::apply);

        var tempTranslator = new ComplexityTranslator();
        tempTranslator.execute(dummyLocation.toFile());

        var exe = Executors.newFixedThreadPool(2);
        var dummyResult = CompletableFuture.supplyAsync( () -> buildRunner.test(dummyLocation), exe);
        var originalResult = CompletableFuture.supplyAsync( () -> buildRunner.test(MainFrame.getSelectedDirectory().toPath()), exe);

        dummyResult.whenComplete((result, error) -> {
            System.out.println("dummy completed");
            System.out.println("result = " + result);
            System.out.println("error = " + error);

            if (error != null) {
                error.printStackTrace();
            }
        });

        dummyResult.thenCombineAsync(originalResult, (dummy, original) -> {
            SwingUtilities.invokeLater(
                    () -> new CompareFrame(originalTranslator, tempTranslator, original, dummy)
            );
            return null;
        }, exe).whenComplete((r, e) -> {
            appliers.forEach(DiffApplier::revert);
            exe.shutdown();
            e.printStackTrace();
        });

    }

    public void applyDiffs(List<DiffPromptResponse> diffs) {
        init();
        for (var diff : diffs) {
            var originalMethod = originalTranslator.getMethod(diff.clazz(), diff.method()).getMethod();
            new DiffApplier(originalMethod, diff).apply();
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
