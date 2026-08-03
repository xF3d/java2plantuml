package ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record DiffPromptResponse(String origin, List<String> newMethods, List<String> notes) {
    public static final DiffPromptResponse EMPTY = new DiffPromptResponse("null", List.of(), List.of());

    public static final String ORIGIN = "-----ORIGIN-----";
    public static final String METHODS = "-----METHODS-----";
    public static final String NOTES = "-----NOTES-----";

    public static DiffPromptResponse fromResponse(String origin, String response) {
        if (response == null) return EMPTY;

        var out = new DiffPromptResponse(origin, new ArrayList<>(), new ArrayList<>());

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


        return new DiffPromptResponse(origin, newMethods, notes);
    }

    public boolean isEmpty() {
        return notes.isEmpty() && newMethods.isEmpty();
    }

    public String original() {
        String origin = ORIGIN + "\n";
        String originValue = origin + "\n";
        String addedMethods = METHODS + "\n";
        String allAddedMethods = String.join("\n", newMethods);
        String notes = "\n" + NOTES + "\n";
        String allNotes = String.join("\n", this.notes);

        return origin + originValue + addedMethods + allAddedMethods + notes + allNotes;
    }

    private static final String SEPARATOR = "---------------------------------------";

    public static String toFileContents(Collection<DiffPromptResponse> responses) {
        return responses.stream().map(DiffPromptResponse::original).collect(Collectors.joining(SEPARATOR));
    }

    public static List<DiffPromptResponse> fromFileContents(String contents) {
        return Arrays.stream(contents.split(Pattern.quote(SEPARATOR))).map(DiffPromptResponse::fromString).toList();
    }
}
