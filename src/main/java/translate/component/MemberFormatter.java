package translate.component;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import translate.translator.UmlTranslator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MemberFormatter {
    public static String fullPackageName(Node node) {
        String packageName = node.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(PackageDeclaration::getName)
                .map(Name::asString)
                .orElse("");

        return packageName + "." + MemberFormatter.fullSimpleName(node);
    }

    public static String nodeClassType(Node node) {
        if (node instanceof ClassOrInterfaceDeclaration c) {
            if (c.isInterface()) {
                return "Interface";
            } else if (!c.isInterface() && c.isAbstract()) {
                return "Abstract class";
            } else {
                return "Class";
            }
        } else if (node instanceof RecordDeclaration) {
            return "Record";
        } else if (node instanceof EnumDeclaration) {
            return "Enum";
        }

        return null;
    }

    // processes potential nested classes names
    public static String fullSimpleName(Node type) {
        if (type instanceof ClassOrInterfaceType cit) {
            return cit.getNameWithScope();
        }

        List<String> names = new ArrayList<>();
        Optional<Node> current = Optional.of(type);

        while (current.isPresent()) {
            Node node = current.get();
            if (node instanceof TypeDeclaration<?> typeDecl) {
                names.add(typeDecl.getNameAsString());
            }
            current = node.getParentNode();
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

        builder.append(attributes(ctor));

        if (UmlTranslator.config.isShowMethods()) {
            for (var constructor : ctor.getConstructors()) {
                builder.append(constructor(constructor));
            }

            builder.append(methods(ctor));
            /*
            for (var method : ctor.getMethods()) {
                builder.append(method(method));
            }*/
        }

        return builder.toString();
    }

    public static String attributes(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        if (UmlTranslator.config.isShowAttributes()) {
            for (var field : ctor.getFields()) {
                builder.append(MemberFormatter.field(field));
            }
        }

        return builder.toString();
    }

    public static String methods(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        if (UmlTranslator.config.isShowMethods()) {
            for (var method : ctor.getMethods()) {
                builder.append(method(method));
            }
        }

        return builder.toString();
    }

    public static String modifiers(NodeList<Modifier> modifiers) {
        StringBuilder sb = new StringBuilder();
        boolean hasVisibility = false;

        for (Modifier mod : modifiers) {
            var keyword = mod.getKeyword();
            switch (keyword) {
                case PUBLIC -> {
                    sb.append("+ ");
                    hasVisibility = true;
                }
                case PRIVATE -> {
                    sb.append("- ");
                    hasVisibility = true;
                }
                case PROTECTED -> {
                    sb.append("# ");
                    hasVisibility = true;
                }
                default -> sb.append("{").append(keyword.asString()).append("} ");
            }
        }

        // If no visibility modifier is present, assume package-private
        if (!hasVisibility) {
            sb.insert(0, "~ ");
        }

        return sb.toString();
    }

    public static <N extends Node, T extends NodeWithModifiers<N> & NodeWithVariables<N>> String field(T field) {
        StringBuilder builder = new StringBuilder();
        String modifiers = modifiers(field.getModifiers());
        for (var variable : field.getVariables()) {
            builder.append(modifiers);
            builder.append(variable(variable));
            builder.append("\n");
        }

        return builder.toString();
    }

    public static <N extends Node, T extends NodeWithSimpleName<N> & NodeWithType<N, ?>> String variable(T declarator) {
        return declarator.getNameAsString() + " : " + declarator.getTypeAsString();
    }

    public static String method(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifiers(method.getModifiers()));
        sb.append(method.getName()).append("(");

        for (Parameter p : method.getParameters()) {
            sb.append(variable(p)).append(", ");
        }

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

        for (Parameter p : ctor.getParameters()) {
            sb.append(variable(p)).append(", ");
        }

        if (!ctor.getParameters().isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(")").append("\n");
        return sb.toString();
    }
}
