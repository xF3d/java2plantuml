package translate.component;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

public class AnnotationWriter extends SetTranslatingComponent<AnnotationDeclaration> {
    public AnnotationWriter() {
        super(AnnotationDeclaration.class);
    }

    @Override
    public UmlEntry writeComponentUML(AnnotationDeclaration element) {
        StringBuilder builder = new StringBuilder();
        builder.append("annotation ");
        builder.append(MemberFormatter.fullSimpleName(element));
        builder.append("{\n");

        for (var member : element.getMembers()) {
            if (member instanceof FieldDeclaration fieldDeclaration) {
                builder.append(MemberFormatter.field(fieldDeclaration));
            } else if (member instanceof AnnotationMemberDeclaration decl) {
                builder.append(decl.getNameAsString() + " : " + decl.getTypeAsString());
                var defaultValueOpt = decl.getDefaultValue();
                if (defaultValueOpt.isPresent()) {
                    // TODO manually parse literal expressions...
                    builder.append("\n");
                } else {
                    builder.append("\n");
                }
            } else {
                throw new RuntimeException("Unhandled annotation member: " + member.getClass());
            }
        }

        builder.append("}\n");

        return new UmlEntry(builder.toString(), "");
    }

    // TODO used for testing, can be removed later
    public @interface Test {
        final static String a = "";

        String a();

        String whatever() default "gigi";
    }
}
