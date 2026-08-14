package ai.reponse;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import translate.translator.SimpleTranslator;

import java.nio.file.Path;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DiffMethodResponse.class, name = "method"),
        @JsonSubTypes.Type(value = DiffClassResponse.class, name = "class")
})
public interface ApplicableDiff {
    void apply(SimpleTranslator originalTranslator, Path toUpdatePath);

    Path getPath(SimpleTranslator translator);

    boolean isEmpty();

    boolean isValid();
}
