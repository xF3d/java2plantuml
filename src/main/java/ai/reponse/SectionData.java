package ai.reponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

public class SectionData {
    private static final Pattern SECTION_PATTERN = compile("^-----([^-]+)-----$");
    private final Map<String, List<String>> sections = new LinkedHashMap<>();

    public boolean hasSections(String... args) {
        for (String arg : args) {
            if (!sections.containsKey(arg)) {
                return false;
            }
        }

        return true;
    }

    public List<String> get(String key) {
        return sections.get(key);
    }

    public static SectionData parseSections(String full) {
        SectionData data = new SectionData();

        if (full == null || full.isBlank()) {
            return data;
        }

        String currentSection = null;

        for (String line : full.lines().toList()) {
            String strip = line.strip();

            if (strip.isEmpty()) {
                continue;
            }

            Matcher matcher = SECTION_PATTERN.matcher(strip);

            if (matcher.matches()) {
                currentSection = matcher.group(1);
                data.sections.putIfAbsent(currentSection, new ArrayList<>());
                continue;
            }

            if (currentSection != null) {
                data.sections.get(currentSection).add(line);
            }
        }

        return data;
    }
}
