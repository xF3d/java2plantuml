package ai;

import java.util.ArrayList;

public record DiffPromptResponse(ArrayList<String> diffs, ArrayList<String> notes) {
    public static final String BEGIN = "-----BEGIN_LINEDIFF-----";
    public static final String END = "-----END_LINEDIFF-----";

    public static DiffPromptResponse fromResponse(String response) {
        var out = new DiffPromptResponse(new ArrayList<>(), new ArrayList<>());

        boolean inDiff = false;

        for (String line : response.lines().toList()) {
            line = line.strip();

            switch (line) {
                case BEGIN -> {
                    inDiff = true;
                    continue;
                }
                case END -> {
                    inDiff = false;
                    continue;
                }
                case "" -> {
                    continue;
                }
            }

            if (inDiff) {
                out.diffs.add(line);
            } else {
                out.notes.add(line);
            }
        }

        return out;
    }

    public String toAi() {
        String beginLineDiff = BEGIN + "\n";
        String allDiffs = String.join("\n", diffs);
        String endLineDiff = "\n" + END + "\n";
        String allNotes = String.join("\n", notes);
        return beginLineDiff + allDiffs + endLineDiff + allNotes;
    }
}
