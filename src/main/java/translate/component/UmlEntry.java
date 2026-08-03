package translate.component;

import java.util.List;

public record UmlEntry(String classDefinition, List<UmlAssociation> associations) {
    public UmlEntry(String classDefinition) {
        this(classDefinition, List.of());
    }
}
