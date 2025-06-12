package translate.component;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import translate.UmlTranslator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MemberFormatter {

    // processes potential nested classes names
    public static String fullSimpleName(TypeDeclaration<?> type) {
        List<String> names = new ArrayList<>();
        Optional<Node> current = Optional.of(type);

        while (current.isPresent() && current.get() instanceof TypeDeclaration<?> currentType) {
            names.add(currentType.getNameAsString());
            current = currentType.getParentNode();
        }

        Collections.reverse(names);
        return String.join("$", names);
    }

    public static String fullSimpleName(ClassOrInterfaceType type) {
        List<String> names = new ArrayList<>();

        ClassOrInterfaceType current = type;
        while (current != null) {
            names.add(current.getNameAsString());
            current = current.getScope().orElse(null);
        }

        Collections.reverse(names);
        return String.join("$", names);
    }

    public static <N extends TypeDeclaration<?>, T extends TypeDeclaration<N> & NodeWithImplements<N>> String nodeWithImplements(T ctor) {
        StringBuilder builder = new StringBuilder();

        for (ClassOrInterfaceType e : ctor.getImplementedTypes()) {
            builder.append(MemberFormatter.fullSimpleName(ctor));
            builder.append(" --|> ");
            builder.append(MemberFormatter.fullSimpleName(e));
            builder.append("\n");
        }

        return builder.toString();
    }

    public static <N extends TypeDeclaration<?>, T extends TypeDeclaration<N> & NodeWithExtends<N>> String nodeWithExtends(T ctor) {
        StringBuilder builder = new StringBuilder();

        for (ClassOrInterfaceType e : ctor.getExtendedTypes()) {
            builder.append(MemberFormatter.fullSimpleName(ctor));
            builder.append(" --|> ");
            builder.append(MemberFormatter.fullSimpleName(e));
            builder.append("\n");
        }

        return builder.toString();
    }

    public static String node(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        if (UmlTranslator.config.isShowAttributes()) {
            for (var field : ctor.getFields()) {
                builder.append(MemberFormatter.field(field));
            }
        }

        if (UmlTranslator.config.isShowMethods()) {
            for (var constructor : ctor.getConstructors()) {
                builder.append(constructor(constructor));
            }

            for (var method : ctor.getMethods()) {
                builder.append(method(method));
            }
        }

        return builder.toString();
    }

    public static String modifiers(NodeList<Modifier> modifiers) {
        StringBuilder sb = new StringBuilder();
        for (Modifier mod : modifiers) {
            var keyword = mod.getKeyword();
            switch (keyword) {
                case PUBLIC -> sb.append("+ ");
                case PRIVATE -> sb.append("- ");
                case PROTECTED -> sb.append("# ");
                case DEFAULT -> sb.append("~ ");
                default -> sb
                        .append("{")
                        .append(keyword.asString())
                        .append("} ");
            }
        }
        return sb.toString();
    }

    public static String field(FieldDeclaration field) {
        return modifiers(field.getModifiers()) +
                field.getVariable(0).getNameAsString() + " : " +
                field.getVariable(0).getTypeAsString() + "\n";
    }

    public static String method(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifiers(method.getModifiers()));
        sb.append(method.getName()).append("(");
        method.getParameters().forEach(p -> sb.append(p.getName()).append(" : ").append(p.getType()).append(", "));
        if (!method.getParameters().isEmpty()) {
            sb.setLength(sb.length() - 2); // remove trailing comma
        }
        sb.append(") : ").append(method.getType().asString()).append("\n");
        return sb.toString();
    }

    public static String constructor(ConstructorDeclaration ctor) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifiers(ctor.getModifiers()));
        sb.append(ctor.getName()).append("(");
        ctor.getParameters().forEach(p -> sb.append(p.getName()).append(" : ").append(p.getType()).append(", "));
        if (!ctor.getParameters().isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(")").append("\n");
        return sb.toString();
    }
}
