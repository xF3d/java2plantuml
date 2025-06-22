package translate.component;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

public class EnumWriter extends SetTranslatingComponent<EnumDeclaration> {
    public EnumWriter() {
        super(EnumDeclaration.class);
    }

    @Override
    public String writeComponent(EnumDeclaration element) {
        StringBuilder sb = new StringBuilder();

        sb.append("enum ");
        sb.append(MemberFormatter.fullSimpleName(element));
        sb.append("{\n");

        for (EnumConstantDeclaration c : element.getEntries()) {
            sb.append(c.getName());
            sb.append("\n");
        }

        sb.append(MemberFormatter.methods(element));

        sb.append(MemberFormatter.nodeWithImplements(element));

        sb.append("}\n");

        return sb.toString();
    }
}
