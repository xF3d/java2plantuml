package ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record DiffPromptResponse(List<String> newMethods, List<String> notes) {
    public static final DiffPromptResponse EMPTY = new DiffPromptResponse(List.of(), List.of());

    public static final String METHODS = "-----METHODS-----";
    public static final String NOTES = "-----NOTES-----";

    public static DiffPromptResponse fromResponse(String response) {
        if (response == null) return EMPTY;

        var out = new DiffPromptResponse(new ArrayList<>(), new ArrayList<>());

        int section = 0;
        for (String line : response.lines().toList()) {
            line = line.strip();

            switch (line) {
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

    public String original() {
        String addedMethods = METHODS + "\n";
        String allAddedMethods = String.join("\n", newMethods);
        String notes = "\n" + NOTES + "\n";
        String allNotes = String.join("\n", this.notes);

        return addedMethods + allAddedMethods + notes + allNotes;
    }

    private static final String SEPARATOR = "---------------------------------------";

    public static String toFileContents(Collection<DiffPromptResponse> responses) {
        return responses.stream().map(DiffPromptResponse::original).collect(Collectors.joining(SEPARATOR));
    }

    public static List<DiffPromptResponse> fromFileContents(String contents) {
        return Arrays.stream(contents.split(Pattern.quote(SEPARATOR))).map(DiffPromptResponse::fromResponse).toList();
    }
}
