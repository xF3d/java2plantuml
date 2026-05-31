package translate.component.formatter;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.AllArgsConstructor;
import translate.structure.PackageManager;
import translate.translator.TranslatorConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// TODO: Parse annotations of classes, methods, and fields and make it configurable
@AllArgsConstructor
public class MemberFormatter {
    // can't change until plantuml decides to stop causing problems
    public static final String PACKAGE_DELIMITER = "::";

    public static final String INNER_CLASS_DELIMITER = ".";

    private final PackageManager packageManager;

    public String fullPackageName(Node node) {
        return fullPackageName(node.findCompilationUnit().orElseThrow(), node);
    }

    public String fullPackageName(CompilationUnit cu, Node node) {
        if (node instanceof TypeDeclaration<?> decl) {
            return PackageManager.resolveTypeDeclaration(decl);
        }

        String simpleName = fullSimpleName(node);
        var result = packageManager.resolveClass(cu, simpleName);
        if (result == null) return "\"external " + simpleName + "\"";

        return result;
    }

    public String nodeClassType(Node node) {
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
    public static String fullSimpleName(Node node) {
        return fullSimpleName(node, INNER_CLASS_DELIMITER);
    }

    public static String fullSimpleName(Node node, String delimiter) {
        if (node instanceof ClassOrInterfaceType cit) {
            return cit.getNameWithScope().replace(".", delimiter);
        }

        List<String> names = new ArrayList<>();
        Optional<Node> current = Optional.of(node);

        while (current.isPresent()) {
            Node nodeTmp = current.get();
            if (nodeTmp instanceof TypeDeclaration<?> typeDecl) {
                names.add(typeDecl.getNameAsString());
            }
            current = nodeTmp.getParentNode();
        }

        Collections.reverse(names);
        return String.join(String.valueOf(delimiter), names);
    }

    public <N extends TypeDeclaration<?>, T extends TypeDeclaration<N> & NodeWithImplements<N>> String nodeWithImplements(T ctor) {
        StringBuilder builder = new StringBuilder();

        CompilationUnit cu = ctor.findCompilationUnit().get();
        for (ClassOrInterfaceType e : ctor.getImplementedTypes()) {
            builder.append(fullPackageName(cu, ctor));
            builder.append(" ..|> ");
            builder.append(fullPackageName(cu, e));
            builder.append("\n");
        }

        return builder.toString();
    }

    public <N extends TypeDeclaration<?>, T extends TypeDeclaration<N> & NodeWithExtends<N>> String nodeWithExtends(T ctor) {
        StringBuilder builder = new StringBuilder();

        CompilationUnit cu = ctor.findCompilationUnit().get();
        for (ClassOrInterfaceType e : ctor.getExtendedTypes()) {
            builder.append(fullPackageName(cu, ctor));
            builder.append(" --|> ");
            builder.append(fullPackageName(cu, e));
            builder.append("\n");
        }

        return builder.toString();
    }

    public String node(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        builder.append(attributes(ctor));

        if (TranslatorConfig.config.isShowMethods()) {
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

    public String attributes(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        if (TranslatorConfig.config.isShowAttributes()) {
            for (var field : ctor.getFields()) {
                builder.append(field(field));
            }
        }

        return builder.toString();
    }

    public String methods(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        if (TranslatorConfig.config.isShowMethods()) {
            for (var method : ctor.getMethods()) {
                builder.append(method(method));
            }
        }

        return builder.toString();
    }

    public String modifiers(NodeList<Modifier> modifiers) {
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

    public <N extends Node, T extends NodeWithModifiers<N> & NodeWithVariables<N>> String field(T field) {
        StringBuilder builder = new StringBuilder();
        String modifiers = modifiers(field.getModifiers());
        for (var variable : field.getVariables()) {
            builder.append(modifiers);
            builder.append(variable(variable));
            builder.append("\n");
        }

        return builder.toString();
    }

    public <N extends Node, T extends NodeWithSimpleName<N> & NodeWithType<N, ?>> String variable(T declarator) {
        return declarator.getNameAsString() + " : " + declarator.getTypeAsString();
    }

    public String method(MethodDeclaration method) {
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

    public String constructor(ConstructorDeclaration ctor) {
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
