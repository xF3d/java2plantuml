package translate.component;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import translate.component.formatter.MemberFormatter;

public class EnumWriter extends SetTranslatingComponent<EnumDeclaration> {
    public EnumWriter() {
        super(EnumDeclaration.class);
    }

    @Override
    public UmlEntry writeComponentUML(EnumDeclaration element, MemberFormatter formatter) {
        StringBuilder sb = new StringBuilder();

        sb.append("enum ");
        sb.append(formatter.fullSimpleName(element));
        sb.append("{\n");

        sb.append("__ Entries __\n");

        for (EnumConstantDeclaration c : element.getEntries()) {
            sb.append(c.getName());
            sb.append("\n");
        }

        sb.append("__ Attributes __\n");

        sb.append(formatter.attributes(element));

        sb.append("__ Methods __\n");

        sb.append(formatter.methods(element));

        sb.append("}\n");

        return new UmlEntry(sb.toString(), formatter.nodeWithImplements(element));
    }
}
