package ai.reponse;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.translator.SimpleTranslator;

import java.nio.file.Path;
import java.util.List;

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

    boolean isUnchanged();

    String origin();

    List<String> notes();

    String explanation();

    String asAi();

    ApplicableDiff withNotes(List<String> notes);

    String getOriginalCode(SimpleTranslator translator);

    static void addOrReplaceMethod(MethodDeclaration newMethod, TypeDeclaration<?> typeDefinition) {
        NodeList<BodyDeclaration<?>> members = typeDefinition.getMembers();

        for (int i = 0; i < members.size(); i++) {
            var m = members.get(i);
            if (m instanceof MethodDeclaration md && md.getSignature().equals(newMethod.getSignature())) {
                members.set(i, newMethod.clone());
                return;
            }
        }

        typeDefinition.addMember(newMethod);
    }
}
