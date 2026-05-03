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
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import translate.ResolverUtils;
import translate.translator.Translator;
import translate.translator.TranslatorConfig;
import translate.translator.UmlTranslator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// TODO: Parse annotations of classes, methods, and fields and make it configurable
public class MemberFormatter {
    public static String fullPackageName(Node node) {
        var cu = node.findCompilationUnit();

        String fullSimpleName =  MemberFormatter.fullSimpleName(node);
        if (node instanceof TypeDeclaration) {
            String packageName = cu
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(PackageDeclaration::getName)
                .map(Name::asString)
                .orElse("");

            if (packageName.isEmpty()) return fullSimpleName;

            return packageName + "." + fullSimpleName;
        }

        /* TODO
         * no way to check if the import is external...
         * manual package framework or remove this partial fix
         */
        var fullImportString = cu
            .map(CompilationUnit::getImports)
            .stream()
            .flatMap(Collection::stream)
            .filter(it -> !it.isAsterisk() && !it.isStatic())
            .map(it -> it.getName().asString())
            .filter(fqn -> fqn.endsWith("." + fullSimpleName.replace('$', '.')))
            .findFirst()
            .orElse(null);

        if (fullImportString != null) {
            return fullImportString.replace(fullSimpleName.replace('$', '.'), fullSimpleName);
        }

        /*
         * can't fix: symbol resolver dies for classes whose generics are external, so can't use
         * extern stereotype reliably, maybe I need to implement a manual package resolution framework
         * to have it be more precise
         */
        String simpleName = MemberFormatter.fullSimpleName(node);
        if (node instanceof Type type) {
            if (type.isPrimitiveType()) return simpleName;

            try {
                String resolved = ResolverUtils.getResolver().toResolvedType(type, ResolvedType.class).describe();
                String resolvedFormat = simpleName.replace('$', '.');
                return resolved.replace(resolvedFormat, simpleName);
            } catch (Exception e) {
            }
        }

        return simpleName;
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
    public static String fullSimpleName(Node node) {
        if (node instanceof ClassOrInterfaceType cit) {
            return cit.getNameWithScope().replace('.', '$');
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
        return String.join("$", names);
    }

    public static <N extends TypeDeclaration<?>, T extends TypeDeclaration<N> & NodeWithImplements<N>> String nodeWithImplements(T ctor) {
        StringBuilder builder = new StringBuilder();

        for (ClassOrInterfaceType e : ctor.getImplementedTypes()) {
            builder.append(MemberFormatter.fullPackageName(ctor));
            builder.append(" ..|> ");
            builder.append(MemberFormatter.fullPackageName(e));
            builder.append("\n");
        }

        return builder.toString();
    }

    public static <N extends TypeDeclaration<?>, T extends TypeDeclaration<N> & NodeWithExtends<N>> String nodeWithExtends(T ctor) {
        StringBuilder builder = new StringBuilder();

        for (ClassOrInterfaceType e : ctor.getExtendedTypes()) {
            builder.append(MemberFormatter.fullPackageName(ctor));
            builder.append(" --|> ");
            builder.append(MemberFormatter.fullPackageName(e));
            builder.append("\n");
        }

        return builder.toString();
    }

    public static String node(NodeWithMembers<?> ctor) {
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

    public static String attributes(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        if (TranslatorConfig.config.isShowAttributes()) {
            for (var field : ctor.getFields()) {
                builder.append(MemberFormatter.field(field));
            }
        }

        return builder.toString();
    }

    public static String methods(NodeWithMembers<?> ctor) {
        StringBuilder builder = new StringBuilder();

        if (TranslatorConfig.config.isShowMethods()) {
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
