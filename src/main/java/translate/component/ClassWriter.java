package translate.component;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class ClassWriter extends SetTranslatingComponent<ClassOrInterfaceDeclaration> {
    public ClassWriter() {
        super(ClassOrInterfaceDeclaration.class);
    }

    @Override
    public void safeAdd(Node object) {
        if (object instanceof ClassOrInterfaceDeclaration decl && !decl.isInterface()) {
            set.add(decl);
        }
    }

    @Override
    public String writeComponentUML(ClassOrInterfaceDeclaration element) {
        StringBuilder builder = new StringBuilder();
        if (element.isAbstract()) {
            builder.append("abstract ");
        }
        builder.append("class ");
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
