package translate.component;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class InterfaceWriter extends SetTranslatingComponent<ClassOrInterfaceDeclaration> {
    protected InterfaceWriter() {
        super(ClassOrInterfaceDeclaration.class);
    }

    @Override
    public void safeAdd(Node object) {
        if (object instanceof ClassOrInterfaceDeclaration decl && decl.isInterface()) {
            set.add(decl);
        }
    }

    @Override
    public String writeComponent(ClassOrInterfaceDeclaration element) {
        StringBuilder builder = new StringBuilder();

        builder.append("interface ");
        builder.append(MemberFormatter.fullSimpleName(element));
        builder.append("{");
        builder.append("\n");

        builder.append(MemberFormatter.node(element));
        //attributes and methods

        builder.append("}\n");

        builder.append(MemberFormatter.nodeWithImplements(element));
        builder.append(MemberFormatter.nodeWithExtends(element));

        return builder.toString();
    }
}
