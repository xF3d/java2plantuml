package translate.translator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitor;
import translate.ResolverUtils;
import translate.structure.PackageManager;
import visitors.PackageManagerVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public interface Translator {

    void addNode(Node node);
    void setError(Boolean b);
    PackageManager getPackageManager();

    default void translateFiles(List<File> files) throws FileNotFoundException {
        ParserConfiguration config = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setSymbolResolver(ResolverUtils.getResolver());

        JavaParser parser = new JavaParser(config);
        PackageManagerVisitor packageManagerVisitor = new PackageManagerVisitor(getPackageManager());

        long start = System.currentTimeMillis();
        // first construct the package map
        for (var f : files) {
            File file = f.getAbsoluteFile();
            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    CompilationUnit cu = result.getResult().get();
                    cu.accept(packageManagerVisitor, null);
                } else {
                    System.err.println("Parsing failed for: " + file.getPath() + "\n");
                    List<Problem> problems = result.getProblems();
                    for (Problem problem : problems) {
                        System.err.println("Problem: " + problem.getMessage());
                        problem.getLocation().ifPresent(loc -> System.out.println(" at " + loc));
                    }
                }
            } catch (FileNotFoundException e) {
                setError(true);
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("Time to create Package Map: " + (end - start));

        // then run the visitors which might depend on it
        for (var f : files) {
            File file = f.getAbsoluteFile();
            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    CompilationUnit cu = result.getResult().get();
                    for (VoidVisitor<Void> visitor : TranslatorConfig.config.getVisitorAdapters()) {
                        cu.accept(visitor, null);
                    }

                } else {
                    System.err.println("Parsing failed for: " + file.getPath() + "\n");
                    List<Problem> problems = result.getProblems();
                    for (Problem problem : problems) {
                        System.err.println("Problem: " + problem.getMessage());
                        problem.getLocation().ifPresent(loc -> System.out.println(" at " + loc));
                    }
                }
            } catch (FileNotFoundException e) {
                setError(true);
                e.printStackTrace();
            }
        }
    }

}