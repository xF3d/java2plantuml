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
    public UmlEntry writeComponentUML(ClassOrInterfaceDeclaration element) {
        StringBuilder classDefinitionBuilder = new StringBuilder();
        if (element.isAbstract()) {
            classDefinitionBuilder.append("abstract ");
        }
        classDefinitionBuilder.append("class ");
        classDefinitionBuilder.append(MemberFormatter.fullSimpleName(element));
        classDefinitionBuilder.append("{");
        classDefinitionBuilder.append("\n");

        classDefinitionBuilder.append(MemberFormatter.node(element));
        //attributes and methods

        classDefinitionBuilder.append("}\n");

        String associations = MemberFormatter.nodeWithImplements(element) +
            MemberFormatter.nodeWithExtends(element);

        return new UmlEntry(classDefinitionBuilder.toString(), associations);
    }
}
