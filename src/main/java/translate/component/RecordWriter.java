package translate.component;

import com.github.javaparser.ast.body.RecordDeclaration;

public class RecordWriter extends SetTranslatingComponent<RecordDeclaration> {
    public RecordWriter() {
        super(RecordDeclaration.class);
    }

    @Override
    public String writeComponentUML(RecordDeclaration element) {
        StringBuilder builder = new StringBuilder();
        builder.append("class ");
        builder.append(MemberFormatter.fullSimpleName(element));
        builder.append("<<record>>");
        builder.append("{");
        builder.append("\n");
        for (var parameter : element.getParameters()) {
            builder.append(MemberFormatter.modifiers(parameter.getModifiers()));
            builder.append(MemberFormatter.variable(parameter));
            builder.append("\n");
        }

        //attributes
        builder.append(MemberFormatter.node(element));

        builder.append("}\n");

        //implemented interfaces
        builder.append(MemberFormatter.nodeWithImplements(element));

        return builder.toString();
    }
}
