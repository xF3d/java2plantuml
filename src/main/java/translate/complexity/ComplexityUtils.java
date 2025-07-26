package translate.complexity;


import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.Getter;

import java.io.File;
import java.util.List;
import java.util.Stack;

public class ComplexityUtils {
    @Getter
    private static SymbolResolver resolver;

    public static void initialize(File projectDirectory) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        addJavaParserTypeSolvers(projectDirectory, typeSolver);


        ParserConfiguration config = new ParserConfiguration();

        resolver = new JavaSymbolSolver(typeSolver);

        config.setSymbolResolver(resolver);
        StaticJavaParser.setConfiguration(config);
    }

    private static void addJavaParserTypeSolvers(File projectDirectory, CombinedTypeSolver typeSolver) {
        File[] files = projectDirectory.listFiles();
        if (files == null) {
            System.out.println("Adding source root: " + projectDirectory.getAbsolutePath());
            typeSolver.add(
                    new JavaParserTypeSolver(
                            sourceOf(projectDirectory)
                    )
            );
            return;
        }

        Stack<File> fileList = new Stack<>();
        fileList.addAll(List.of(files));

        while (!fileList.isEmpty()) {
            File sub = fileList.pop();

            File srcMainJava = sourceOf(sub);
            if (srcMainJava.exists() && srcMainJava.isDirectory()) {
                System.out.println("Adding source root: " + srcMainJava.getAbsolutePath());
                typeSolver.add(new JavaParserTypeSolver(srcMainJava));
                continue;
            }

            var subFiles = sub.listFiles();
            if (subFiles != null) {
                fileList.addAll(List.of(subFiles));
            }
        }
    }

    public static ResolvedReferenceTypeDeclaration resolve(Node node) {
        return resolver.resolveDeclaration(node, ResolvedReferenceTypeDeclaration.class);
    }

    public static <T> T resolve(Node node, Class<T> clazz) {
        return resolver.resolveDeclaration(node, clazz);
    }

    public static File sourceOf(File file) {
        return new File(file, "src/main/java");
    }
}
