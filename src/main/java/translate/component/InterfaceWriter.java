package translate.component;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class InterfaceWriter extends SetTranslatingComponent<ClassOrInterfaceDeclaration> {
    public InterfaceWriter() {
        super(ClassOrInterfaceDeclaration.class);
    }

    @Override
    public void safeAdd(Node object) {
        if (object instanceof ClassOrInterfaceDeclaration decl && decl.isInterface()) {
            set.add(decl);
        }
    }

    @Override
    public UmlEntry writeComponentUML(ClassOrInterfaceDeclaration element, MemberFormatter formatter) {
        StringBuilder builder = new StringBuilder();

        builder.append("interface ");
        builder.append(formatter.fullSimpleName(element));
        builder.append("{");
        builder.append("\n");

        builder.append(formatter.node(element));
        //attributes and methods

        builder.append("}\n");

        return new UmlEntry(builder.toString(), formatter.nodeWithExtends(element));
    }
}
