package translate.translator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupedTranslators {
    private final UmlTranslator umlTranslator = new UmlTranslator();
    private final ComplexityTranslator complexityTranslator = new ComplexityTranslator();
}
