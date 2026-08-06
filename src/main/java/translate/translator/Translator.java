package translate.translator;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitor;
import file.Config;
import file.DirectoryExplorer;
import file.FileHandler;
import file.FileLogger;
import translate.structure.pkg.PackageManager;
import util.ProjectJavaParser;
import visitors.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public interface Translator {

    void addNode(Node node);
    void setError(Boolean b);
    PackageManager getPackageManager();
    boolean hasExecuted();

    default void handleCompilationUnit(CompilationUnit unit) {

    }

    default void translateFiles(List<File> files) throws FileNotFoundException {
        PackageManagerVisitor packageManagerVisitor = new PackageManagerVisitor(getPackageManager());

        List<CompilationUnit> compilationUnits = new ArrayList<>();

        long start = System.currentTimeMillis();
        // Parse each file once and cache the CompilationUnits
        for (var f : files) {
            File file = f.getAbsoluteFile();
            try {
                ParseResult<CompilationUnit> result = ProjectJavaParser.PARSER.parse(file);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    compilationUnits.add(result.getResult().get());
                } else {
                    FileLogger.LOGGER.warning("Parsing failed for: " + file.getPath() + "\n");
                    List<Problem> problems = result.getProblems();
                    for (Problem problem : problems) {
                        FileLogger.LOGGER.warning("Problem: " + problem.getMessage());
                        problem.getLocation().ifPresent(loc -> FileLogger.LOGGER.warning(" at " + loc));
                    }
                }
            } catch (FileNotFoundException e) {
                setError(true);
                e.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();
        FileLogger.LOGGER.info("Time to parse sources: " + (end - start));

        start = System.currentTimeMillis();
        // first construct the package map
        for (var cu : compilationUnits) {
                cu.accept(packageManagerVisitor, null);
        }
        end = System.currentTimeMillis();
        FileLogger.LOGGER.info("Time to create Package Map: " + (end - start));

        start = System.currentTimeMillis();
        // then run the visitors which might depend on it
        for (CompilationUnit cu : compilationUnits) {
            handleCompilationUnit(cu);
            for (VoidVisitor<Void> visitor : Config.getInstance().getVisitorsBuilder().createVisitors(this)) {
                cu.accept(visitor, null);
            }
        }
        end = System.currentTimeMillis();
        FileLogger.LOGGER.info("Time to visit type definitions: " + (end - start));
    }

    default boolean execute(File selectedDirectory) {
        if (hasExecuted()) return true;

        FileHandler handler = new FileHandler(this);

        if (selectedDirectory == null || !selectedDirectory.exists()) {
            return false;
        }

        new DirectoryExplorer(handler).explore(selectedDirectory);
        return true;
    }
}