package translate.component;

import com.github.javaparser.ast.body.RecordDeclaration;
import translate.component.formatter.MemberFormatter;

public class RecordWriter extends SetTranslatingComponent<RecordDeclaration> {
    public RecordWriter() {
        super(RecordDeclaration.class);
    }

    @Override
    public UmlEntry writeComponentUML(RecordDeclaration element, MemberFormatter formatter) {
        StringBuilder builder = new StringBuilder();
        builder.append("record ");
        builder.append(MemberFormatter.fullSimpleName(element));
        builder.append("{");
        builder.append("\n");
        for (var parameter : element.getParameters()) {
            builder.append(formatter.modifiers(parameter.getModifiers()));
            builder.append(formatter.variable(parameter));
            builder.append("\n");
        }

        //attributes
        builder.append(formatter.node(element));

        builder.append("}\n");

        return new UmlEntry(builder.toString(), formatter.nodeWithImplements(element));
    }
}
