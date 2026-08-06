package ai;

import com.github.javaparser.ast.body.MethodDeclaration;
import util.ProjectJavaParser;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record DiffPromptResponse(String clazz, String method, List<String> newMethods, List<String> notes) {
    public static final DiffPromptResponse EMPTY = new DiffPromptResponse("null", "null", List.of(), List.of());

    public static final String ORIGIN = "-----ORIGIN-----";
    public static final String METHODS = "-----METHODS-----";
    public static final String NOTES = "-----NOTES-----";

    public static DiffPromptResponse fromResponse(String origin, String response) {
        if (origin == null) throw new IllegalArgumentException("Null origin");

        String[] split = origin.split(Pattern.quote("#"));
        if (split.length != 2) throw new IllegalArgumentException("Illegal origin: " + origin);

        return fromResponse(split[0], split[1], response);
    }

    public static DiffPromptResponse fromResponse(String clazz, String method, String response) {
        if (response == null) return EMPTY;

        var out = new DiffPromptResponse(clazz, method, new ArrayList<>(), new ArrayList<>());

        int section = 0;
        for (String line : response.lines().toList()) {
            String strip = line.strip();

            switch (strip) {
                case METHODS -> {
                    section = 0;
                    continue;
                }
                case NOTES -> {
                    section = 1;
                    continue;
                }

                case "" -> {
                    continue;
                }
            }

            if (section == 0) {
                out.newMethods.add(line);
            } else {
                out.notes.add(line);
            }
        }


        return out;
    }

    public static DiffPromptResponse fromString(String full) {
        if (full == null) return EMPTY;

        ArrayList<String> newMethods = new ArrayList<>();
        ArrayList<String> notes = new ArrayList<>();


        String origin = null;
        int section = 0;
        for (String line : full.lines().toList()) {
            String strip = line.strip();

            switch (strip) {
                case ORIGIN -> {
                    section = 2;
                    continue;
                }
                case METHODS -> {
                    section = 0;
                    continue;
                }
                case NOTES -> {
                    section = 1;
                    continue;
                }

                case "" -> {
                    continue;
                }
            }

            if (section == 0) {
                newMethods.add(line);
            } else if (section == 1) {
                notes.add(line);
            } else {
                origin = line;
            }
        }

        if (origin == null) return EMPTY;

        String[] split = origin.split(Pattern.quote("#"));
        if (split.length != 2) return EMPTY;

        return new DiffPromptResponse(split[0], split[1], newMethods, notes);
    }

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

    public boolean isValid() {
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

    private static final String SEPARATOR = "---------------------------------------";

    public static String toFileContents(Collection<DiffPromptResponse> responses) {
        return responses.stream().map(DiffPromptResponse::asSave).collect(Collectors.joining(SEPARATOR + "\n"));
    }

    public static List<DiffPromptResponse> fromFileContents(String contents) {
        return Arrays.stream(contents.split(Pattern.quote(SEPARATOR))).map(DiffPromptResponse::fromString).toList();
    }
}
