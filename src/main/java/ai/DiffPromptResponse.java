package ai;

import java.util.ArrayList;
import java.util.List;

public record DiffPromptResponse(List<String> diffs, List<String> newMethods, List<String> notes) {
    public static final DiffPromptResponse EMPTY = new DiffPromptResponse(List.of(), List.of(), List.of());

    public static final String LINEDIFF = "-----LINEDIFF-----";
    public static final String ADDED_METHODS = "-----ADDED_METHODS-----";
    public static final String NOTES = "-----NOTES-----";

    public static DiffPromptResponse fromResponse(String response) {
        if (response == null) return EMPTY;

        var out = new DiffPromptResponse(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        int section = 0;
        for (String line : response.lines().toList()) {
            line = line.strip();

            switch (line) {
                case LINEDIFF -> {
                    section = 0;
                    continue;
                }
                case ADDED_METHODS -> {
                    section = 1;
                    continue;
                }
                case NOTES -> {
                    section = 2;
                    continue;
                }

                case "" -> {
                    continue;
                }
            }

            if (section == 0) {
                out.diffs.add(line);
            } else if (section == 1) {
                out.newMethods.add(line);
            } else if (section == 2) {
                out.notes.add(line);
            }
        }

        return out;
    }

    public String toAi() {
        String lineDiff = LINEDIFF + "\n";
        String allDiffs = String.join("\n", diffs);
        String addedMethods = "\n" + ADDED_METHODS + "\n";
        String allAddedMethods = String.join("\n", newMethods);
        String notes = "\n" + NOTES + "\n";
        String allNotes = String.join("\n", this.notes);

        return lineDiff + allDiffs + addedMethods + allAddedMethods + notes + allNotes;
    }
}
