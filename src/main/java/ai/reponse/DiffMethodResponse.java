package ai.reponse;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import file.FileLogger;
import translate.translator.SimpleTranslator;
import util.ProjectJavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record DiffMethodResponse(
        String clazz,
        String method,
        List<String> newMethods,
        List<String> notes
) implements ApplicableDiff {
    public static final DiffMethodResponse EMPTY = new DiffMethodResponse("null", "null", List.of(), List.of());

    public static final String ORIGIN = "-----ORIGIN-----";
    public static final String METHODS = "-----METHODS-----";
    public static final String NOTES = "-----NOTES-----";

    public static DiffMethodResponse fromResponse(String origin, String response) {
        if (origin == null) throw new IllegalArgumentException("Null origin");

        String[] split = origin.split(Pattern.quote("#"));
        if (split.length != 2) throw new IllegalArgumentException("Illegal origin: " + origin);

        return fromResponse(split[0], split[1], response);
    }

    public static DiffMethodResponse fromResponse(String clazz, String method, String response) {
        if (response == null) return EMPTY;

        SectionData data = SectionData.parseSections(response);

        if (!data.hasSections("METHODS", "NOTES")) return EMPTY;

        List<String> methods = data.get("METHODS");
        List<String> notes = data.get("NOTES");

        return new DiffMethodResponse(clazz, method, methods, notes);
    }

    public static DiffMethodResponse fromString(String full) {
        if (full == null) return EMPTY;

        SectionData data = SectionData.parseSections(full);

        if (!data.hasSections("ORIGIN", "METHODS", "NOTES")) return EMPTY;

        List<String> originLines = data.get("ORIGIN");
        List<String> methods = data.get("METHODS");
        List<String> notes = data.get("NOTES");

        if (originLines.size() != 1) return EMPTY;

        String[] split = originLines.getFirst().split(Pattern.quote("#"));

        if (split.length != 2) return EMPTY;

        return new DiffMethodResponse(
                split[0],
                split[1],
                new ArrayList<>(methods),
                new ArrayList<>(notes)
        );
    }

    @Override
    public boolean isEmpty() {
        return notes.isEmpty() && newMethods.isEmpty();
    }

    public String codeMethods() {
        return String.join("\n", newMethods);
    }

    public String origin() {
        return clazz + "#" + method;
    }

    public String asSave() {
        String origin = ORIGIN + "\n";
        String originValue = this.origin() + "\n";
        String addedMethods = METHODS + "\n";
        String allAddedMethods = String.join("\n", newMethods);
        String notes = "\n" + NOTES + "\n";
        String allNotes = String.join("\n", this.notes);

        return origin + originValue + addedMethods + allAddedMethods + notes + allNotes;
    }

    @Override
    public boolean isValid() {
        if (isEmpty()) return false;

        String toParser = "class DUMMY {\n" + codeMethods() + "\n}";
        var cuResult = ProjectJavaParser.PARSER.parse(toParser);
        if (!cuResult.isSuccessful()) return false;

        var resultOpt = cuResult.getResult();
        if (resultOpt.isEmpty()) return false;

        var result = resultOpt.get();
        return result.findAll(MethodDeclaration.class)
                .stream()
                .anyMatch(decl -> decl.getSignature().asString().equals(method));
    }

    public List<MethodDeclaration> getAddedMethods() {
        ArrayList<MethodDeclaration> addedMethods = new ArrayList<>();
        String toParse = "class DUMMY {\n" + codeMethods() + "}\n";
        ParseResult<CompilationUnit> result = ProjectJavaParser.PARSER.parse(toParse);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            CompilationUnit codeCu = result.getResult().get();
            codeCu.findAll(MethodDeclaration.class, addedMethods::add);
            return addedMethods;
        } else {
            FileLogger.LOGGER.warning("Invalid Apply Patch: Parsing");
            return List.of();
        }
    }

    @Override
    public void apply(SimpleTranslator originalTranslator, Path toUpdatePath) {
        if (!isValid()) {
            FileLogger.LOGGER.warning("Invalid Apply Patch: Invalid");
            return;
        }

        CompilationUnit cu;
        ParseResult<CompilationUnit> parseResult;
        try {
            parseResult = ProjectJavaParser.PARSER.parse(toUpdatePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (parseResult.isSuccessful()) {
            cu = parseResult.getResult().orElseThrow();
        } else {
            cu = null;
        }

        var originalMethod = originalTranslator.getMethodDeclaration(clazz, method);
        List<MethodDeclaration> addedMethods = getAddedMethods();

        // find declaring class
        TypeDeclaration<?> clazz = originalMethod.findAncestor(TypeDeclaration.class)
                .flatMap(c -> cu.findFirst(TypeDeclaration.class,
                        cc -> cc.getFullyQualifiedName().equals(c.getFullyQualifiedName())))
                .orElseThrow(() -> new IllegalStateException("Target class not found"));

        MethodDeclaration replacement = null;
        for (MethodDeclaration method : addedMethods) {
            if (method.getSignature().equals(originalMethod.getSignature())) {
                replacement = method;
            }
        }

        // should never happen
        if (replacement == null) {
            FileLogger.LOGGER.warning("Invalid Patch: at Apply");
            return;
        }

        NodeList<BodyDeclaration<?>> members = clazz.getMembers();

        for (int i = 0; i < members.size(); i++) {
            if (members.get(i) instanceof MethodDeclaration md &&
                    md.getSignature().equals(originalMethod.getSignature())) {
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
    public Path getPath(SimpleTranslator translator) {
        return translator.getMethodDeclaration(clazz, method)
                .findCompilationUnit().orElseThrow()
                .getStorage().orElseThrow()
                .getPath();
    }

    private static final String SEPARATOR = "---------------------------------------";

    public static String toFileContents(Collection<DiffMethodResponse> responses) {
        return responses.stream().map(DiffMethodResponse::asSave).collect(Collectors.joining(SEPARATOR + "\n"));
    }

    public static List<DiffMethodResponse> fromFileContents(String contents) {
        return Arrays.stream(contents.split(Pattern.quote(SEPARATOR))).map(DiffMethodResponse::fromString).toList();
    }
}
