package translate.component;

import java.util.List;
import java.util.Map;

public record UmlPackageEntry(Map<String, List<String>> packageMap, List<UmlAssociation> associations) {
}
