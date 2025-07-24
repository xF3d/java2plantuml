package translate.translator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitor;
import translate.ClassDiagramConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public interface Translator {

    void addNode(Node node);
    void setError(Boolean b);
    ClassDiagramConfig getConfig();

    default void translateFile(File f) throws FileNotFoundException {
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        File file = f.getAbsoluteFile();
        try {
            ParseResult<CompilationUnit> result = parser.parse(file);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                for (VoidVisitor<Void> visitor : getConfig().getVisitorAdapters()) {
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