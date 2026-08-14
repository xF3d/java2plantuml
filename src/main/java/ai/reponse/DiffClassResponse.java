package ai.reponse;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import file.FileLogger;
import translate.translator.SimpleTranslator;
import util.ProjectJavaParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record DiffClassResponse(
        String clazz,
        List<String> addedImports,
        List<String> changes,
        List<String> added,
        List<String> notes
) implements ApplicableDiff {

    public static final DiffClassResponse EMPTY = new DiffClassResponse("null", List.of(), List.of(), List.of(), List.of());

    private static final String SEPARATOR = "===============";

    public static DiffClassResponse fromResponse(String clazz, String response) {
        var data = SectionData.parseSections(response);

        if (!data.hasSections("CHANGES")) {
            return EMPTY;
        }

        List<String> toProcess = data.orElse("ADDED", List.of());
        List<StringBuilder> codeBuilder = new ArrayList<>();

        for (var line : toProcess) {
            if (codeBuilder.isEmpty()) {
                codeBuilder.add(new StringBuilder(line));
                continue;
            }

            if (line.strip().equals(SEPARATOR)) {
                codeBuilder.add(new StringBuilder(line));
            } else {
                codeBuilder.getLast().append(line);
            }
        }

        return new DiffClassResponse(
                clazz,
                data.orElse("ADDED_IMPORTS", List.of()),
                data.get("CHANGES"),
                codeBuilder.stream().map(StringBuilder::toString).toList(),
                data.orElse("NOTES", List.of())
        );
    }

    @Override
    public void apply(SimpleTranslator originalTranslator, Path toUpdatePath) {
        if (!isValid()) {
            FileLogger.LOGGER.warning("Invalid Apply Patch Class: Invalid");
            return;
        }

        Path projectRoot = getJavaRoot(toUpdatePath);
        List<CompilationUnit> addedClasses = parseAdded();

        for (var clazz : addedClasses) {
            applyAddClass(projectRoot, clazz);
        }

        TypeDeclaration<?> originalClazz = originalTranslator.getTypeDeclaration(clazz);
        CompilationUnit originalCu = originalClazz.findCompilationUnit().orElseThrow();

        NodeList<ImportDeclaration> imports = parseImports();
        if (imports == null) throw new RuntimeException("impossible");
        imports.forEach(originalCu::addImport);

        CompilationUnit changes = parseChanges();
        if (changes == null) throw new RuntimeException("impossible");
        changes.findAll(MethodDeclaration.class).forEach(originalClazz::addMember);
        changes.findAll(FieldDeclaration.class).forEach(originalClazz::addMember);

        try {
            Files.writeString(toUpdatePath, originalCu.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyAddClass(Path toAdd, CompilationUnit cu) {
        String packageDecl = cu.getPackageDeclaration()
                .map(PackageDeclaration::getName)
                .map(Name::asString)
                .orElse("");

        var typeDecl = cu.findFirst(TypeDeclaration.class);
        var nameDecl = typeDecl.orElseThrow().getNameAsString();

        Path newFile;
        if (packageDecl.isEmpty()) {
            newFile = toAdd.resolve(nameDecl + ".java");
        } else {
            newFile = toAdd.resolve(packageDecl.replace(".", File.separator));
        }

        try {
            Files.writeString(newFile, cu.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getJavaRoot(Path path) {
        Path current = path;

        while (current != null) {
            Path fileName = current.getFileName();
            if (fileName != null && fileName.toString().equals("java")) {
                return current;
            }
            current = current.getParent();
        }

        throw new IllegalArgumentException("Could not find 'java' directory from: " + path);
    }

    @Override
    public Path getPath(SimpleTranslator translator) {
        return translator.getTypeDeclaration(clazz)
                .findCompilationUnit().orElseThrow()
                .getStorage().orElseThrow()
                .getPath();
    }

    @Override
    public boolean isEmpty() {
        return changes.isEmpty() && added.isEmpty() && notes.isEmpty();
    }

    @Override
    public boolean isValid() {
        return parseChanges() != null && parseAdded().size() == added.size() && parseImports() != null;
    }

    public CompilationUnit parseChanges() {
        String toParser = "class DUMMY {\n" + String.join("\n", changes) + "\n}";

        var cuResult = ProjectJavaParser.PARSER.parse(toParser);
        if (cuResult.isSuccessful()) {
            return cuResult.getResult().orElseThrow();
        }

        return null;
    }

    public NodeList<ImportDeclaration> parseImports() {
        String toParse = String.join("\n", addedImports) + "\n class DUMMY {}";
        ParseResult<CompilationUnit> parsedImports = ProjectJavaParser.PARSER.parse(toParse);

        if (parsedImports.isSuccessful()) {
            return parsedImports.getResult().orElseThrow().getImports();
        }

        return null;
    }

    public List<CompilationUnit> parseAdded() {
        List<CompilationUnit> output = new ArrayList<>(added.size());

        for (var file : added) {
            var cuResult = ProjectJavaParser.PARSER.parse(file);
            if (cuResult.isSuccessful()) {
                output.add(
                        cuResult.getResult().orElse(null)
                );
            }
        }

        return output;
    }

    @Override
    public String origin() {
        return clazz;
    }
}
