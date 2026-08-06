package translate.dummy;

import ai.DiffPromptResponse;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.translator.ComplexityTranslator;
import util.ProjectJavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DiffApplier implements ReversibleAction {
    private MethodDeclaration original;
    private List<MethodDeclaration> addedMethods;
    private Path toUpdatePath;

    public DiffApplier(MethodDeclaration original, DiffPromptResponse diff, ComplexityTranslator toUpdateTranslator) throws IllegalStateException {
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

        this.toUpdatePath = toUpdateTranslator.getMethod(diff.clazz(), diff.method()).getMethod()
                .findCompilationUnit().orElseThrow()
                .getStorage().orElseThrow()
                .getPath();
    }

    public DiffApplier(MethodDeclaration original, DiffPromptResponse diff) throws IllegalStateException {
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


    @Override
    public void apply() {
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

    @Override
    public void revert() {
        CompilationUnit cu = parsePath();

        TypeDeclaration<?> clazz = getOwningClass(cu, original);

        clazz.getMethods().removeIf(md ->
                addedMethods.stream()
                        .filter(m -> !m.getSignature().equals(original.getSignature()))
                        .anyMatch(m -> md.getSignature().equals(m.getSignature())));

        NodeList<BodyDeclaration<?>> members = clazz.getMembers();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i) instanceof MethodDeclaration md &&
                    md.getSignature().equals(original.getSignature())) {
                members.set(i, original.clone());
                break;
            }
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
