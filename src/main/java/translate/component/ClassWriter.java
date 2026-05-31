package translate.component;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import translate.component.formatter.MemberFormatter;

import java.util.regex.Pattern;

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
    public UmlEntry writeComponentUML(ClassOrInterfaceDeclaration element, MemberFormatter formatter) {
        StringBuilder classDefinitionBuilder = new StringBuilder();

        if (isExceptionClass(element)) {
            classDefinitionBuilder.append("exception ");
        } else {
            if (element.isAbstract()) {
                classDefinitionBuilder.append("abstract ");
            }

            classDefinitionBuilder.append("class ");
        }

        classDefinitionBuilder.append(formatter.fullSimpleName(element));
        classDefinitionBuilder.append("{");
        classDefinitionBuilder.append("\n");

        classDefinitionBuilder.append(formatter.node(element));
        //attributes and methods

        classDefinitionBuilder.append("}\n");

        String associations = formatter.nodeWithImplements(element) +
            formatter.nodeWithExtends(element);

        return new UmlEntry(classDefinitionBuilder.toString(), associations);
    }


    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(Pattern.quote("exception"), Pattern.CASE_INSENSITIVE);
    private static final Pattern THROWABLE_PATTERN = Pattern.compile(Pattern.quote("throwable"), Pattern.CASE_INSENSITIVE);

    private boolean isExceptionClass(ClassOrInterfaceDeclaration element) {
        if (EXCEPTION_PATTERN.matcher(element.getNameAsString()).find()) return true;
        if (THROWABLE_PATTERN.matcher(element.getNameAsString()).find()) return true;

        for (var extended : element.getExtendedTypes()) {
            if (EXCEPTION_PATTERN.matcher(extended.getNameAsString()).find()) return true;
            if (THROWABLE_PATTERN.matcher(extended.getNameAsString()).find()) return true;
        }

        return false;
    }
}
