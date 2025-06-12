package translate.component;

import com.github.javaparser.ast.body.RecordDeclaration;

public class RecordWriter extends SetTranslatingComponent<RecordDeclaration> {
    public RecordWriter() {
        super(RecordDeclaration.class);
    }

    @Override
    public void writeComponent(RecordDeclaration element, StringBuilder builder) {
        builder.append("class ");
        builder.append(element.getName());
        builder.append("<<record>>");
        builder.append("{");
        builder.append("\n");

        //attributes
        builder.append(MemberFormatter.node(element));

        builder.append("}\n");

        //implemented interfaces
        builder.append(MemberFormatter.nodeWithImplements(element));
    }
}
