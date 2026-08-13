package translate.dummy;

import ai.reponse.DiffMethodResponse;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import file.FileLogger;
import translate.translator.ComplexityTranslator;
import util.ProjectJavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DiffApplier {
    private MethodDeclaration original;
    private List<MethodDeclaration> addedMethods;
    private Path toUpdatePath;

    public DiffApplier(MethodDeclaration original, DiffMethodResponse diff, ComplexityTranslator toUpdateTranslator) throws IllegalStateException {
        this.original = original;

        this.addedMethods = new ArrayList<>();
        String toParse = "class DUMMY {\n" + diff.codeMethods() + "}\n";
        ParseResult<CompilationUnit> result = ProjectJavaParser.PARSER.parse(toParse);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            CompilationUnit cu = result.getResult().get();
            cu.findAll(MethodDeclaration.class, addedMethods::add);
        } else {
            throw new IllegalStateException("Invalid parsing");
        }

        this.toUpdatePath = toUpdateTranslator.getMethodDeclaration(diff.clazz(), diff.method())
                .findCompilationUnit().orElseThrow()
                .getStorage().orElseThrow()
                .getPath();
    }

    public DiffApplier(MethodDeclaration original, DiffMethodResponse diff) throws IllegalStateException {
        this.original = original;

        this.addedMethods = new ArrayList<>();
        String toParse = "class DUMMY {\n" + diff.codeMethods() + "\n}";
        ParseResult<CompilationUnit> result = ProjectJavaParser.PARSER.parse(toParse);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            CompilationUnit cu = result.getResult().get();
            cu.findAll(MethodDeclaration.class, addedMethods::add);
        } else {
            throw new IllegalStateException("Invalid parsing");
        }

        this.toUpdatePath = original
                .findCompilationUnit().orElseThrow()
                .getStorage().orElseThrow()
                .getPath();
    }


    public void apply() {
        if (!isValid()) {
            FileLogger.LOGGER.warning("Invalid Patch: at Apply");
            return;
        }

        CompilationUnit cu = parsePath();

        TypeDeclaration<?> clazz = getOwningClass(cu, original);

        MethodDeclaration replacement = findReplacement();

        NodeList<BodyDeclaration<?>> members = clazz.getMembers();

        for (int i = 0; i < members.size(); i++) {
            if (members.get(i) instanceof MethodDeclaration md &&
                    md.getSignature().equals(original.getSignature())) {
                members.set(i, replacement.clone());
                break;
            }
        }

        for (MethodDeclaration method : addedMethods) {
            if (method == replacement) {
                continue;
            }
            clazz.addMember(method.clone());
        }

        try {
            Files.writeString(toUpdatePath, cu.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static TypeDeclaration<?> getOwningClass(CompilationUnit cu, MethodDeclaration method) {
        return method.findAncestor(TypeDeclaration.class)
                .flatMap(c -> cu.findFirst(TypeDeclaration.class,
                        cc -> cc.getFullyQualifiedName().equals(c.getFullyQualifiedName())))
                .orElseThrow(() -> new IllegalStateException("Target class not found"));
    }

    private MethodDeclaration findReplacement() {
        for (MethodDeclaration method : addedMethods) {
            if (method.getSignature().equals(original.getSignature())) {
                return method;
            }
        }

        throw new IllegalStateException("Replacement method not found: " + original.getSignature());
    }

    public boolean isValid() {
        try {
            findReplacement();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private CompilationUnit parsePath() {
        try {
            var parseResult = ProjectJavaParser.PARSER.parse(toUpdatePath);
            if (parseResult.isSuccessful()) {
                return parseResult.getResult().orElseThrow();
            }

            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
