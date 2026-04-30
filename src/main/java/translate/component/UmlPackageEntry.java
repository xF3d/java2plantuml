package translate.component;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

public record UmlPackageEntry(Map<String, List<String>> packageMap, List<String> associations) {
}
