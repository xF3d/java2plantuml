package translate.component;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

public class EnumWriter extends SetTranslatingComponent<EnumDeclaration> {
    public EnumWriter() {
        super(EnumDeclaration.class);
    }

    @Override
    public String writeComponentUML(EnumDeclaration element) {
        StringBuilder sb = new StringBuilder();

        sb.append("enum ");
        sb.append(MemberFormatter.fullSimpleName(element));
        sb.append("{\n");

        sb.append("__ Entries __\n");

        for (EnumConstantDeclaration c : element.getEntries()) {
            sb.append(c.getName());
            sb.append("\n");
        }

        sb.append("__ Attributes __\n");

        sb.append(MemberFormatter.attributes(element));

        sb.append("__ Methods __\n");

        sb.append(MemberFormatter.methods(element));

        sb.append("}\n");

        sb.append(MemberFormatter.nodeWithImplements(element));

        return sb.toString();
    }
}
