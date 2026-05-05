package translate.structure;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import translate.component.MemberFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageManager {

    private static final String ASTERISK_FORMAT = MemberFormatter.PACKAGE_DELIMITER + "*";
    private static final String STATIC_IDENTIFIER = "#";

    static class PackageNode {
        String name;
        PackageNode parent;
        Map<String, PackageNode> children = new HashMap<>();
        Map<String, ClassNode> classes = new HashMap<>();

        PackageNode(String name, PackageNode parent) {
            this.name = name;
            this.parent = parent;
        }

        public void addClass(String fullInnerClazz) {
            String[] split = fullInnerClazz.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);

            ClassNode foundBaseClass = classes.get(split[0]);
            if (foundBaseClass == null) {
                ClassNode classNode = new ClassNode(split[0]);
                classNode.addInner(split);
                classes.put(split[0], classNode);
            } else {
                foundBaseClass.addInner(split);
            }
        }

        public boolean containsClass(String fullInnerClazz) {
            String[] split = fullInnerClazz.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);
            return classes.containsKey(split[0]);
        }

        public boolean containsClassStatic(String fullInnerClazz) {
            String[] split = fullInnerClazz.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);
            ClassNode foundBaseClass = classes.get(split[0]);

            if (foundBaseClass == null) return false;

            return foundBaseClass.containsStatic(fullInnerClazz);
        }
    }

    static class ClassNode {
        String name;
        Map<String, ClassNode> innerClasses = new HashMap<>();

        ClassNode(String name) {
            this.name = name;
        }

        public void addInner(String[] inners) {
            // first element is just the name itself, so we can skip
            if (inners.length == 1) return;

            ClassNode current = this;
            for (int i = 1; i < inners.length; i++) {
                current.innerClasses.putIfAbsent(inners[i], new ClassNode(inners[i]));
                current = current.innerClasses.get(inners[i]);
            }
        }

        public boolean containsStatic(String fullInnerClazz) {
            String[] parts = fullInnerClazz.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);

            ClassNode current = this;

            if (!current.name.equals(parts[0])) return false;

            for (int i = 1; i < parts.length; i++) {
                current = current.innerClasses.get(parts[i]);
                if (current == null) return false;
            }

            return true;
        }
    }

    private final PackageNode root = new PackageNode("", null);

    public void addClass(String fqn) {
        String[] parts = fqn.split("\\" + MemberFormatter.PACKAGE_DELIMITER);
        PackageNode current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            PackageNode before = current;
            current.children.putIfAbsent(parts[i], new PackageNode(parts[i], before));
            current = current.children.get(parts[i]);
        }

        current.addClass(parts[parts.length - 1]);
    }

    private PackageNode getPackageNode(String packageName) {
        if (packageName == null || packageName.isEmpty()) return root;

        String[] parts = packageName.split("\\" + MemberFormatter.PACKAGE_DELIMITER);
        PackageNode current = root;

        for (String part : parts) {
            current = current.children.get(part);
            if (current == null) return null;
        }

        return current;
    }

    /**
     *
     * @param packageLocation where does the class resolution start, to scan local classes
     * @param importList list of all imports
     * @param className full class name with proper inner classes formatting
     * @return fqn of the className
     */
    public String resolveClass(String packageLocation, List<String> importList, String className) {
        // 1. Explicit (non-static, non-wildcard) imports
        for (String imp : importList) {
            if (imp.startsWith(STATIC_IDENTIFIER) || imp.endsWith(ASTERISK_FORMAT)) continue;

            int lastDot = imp.lastIndexOf(MemberFormatter.PACKAGE_DELIMITER);
            if (lastDot == -1) continue;

            String pkg = imp.substring(0, lastDot);
            String importedClass = imp.substring(lastDot + 1);

            String[] parts = className.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);
            String base = parts[0];

            if (importedClass.equals(base)) return pkg + MemberFormatter.PACKAGE_DELIMITER + className;
        }

        // 2. Same package
        PackageNode currentPkg = getPackageNode(packageLocation);
        if (currentPkg != null && currentPkg.containsClass(className)) {
            return packageLocation + MemberFormatter.PACKAGE_DELIMITER + className;
        }

        // 3. Wildcard imports
        for (String imp : importList) {
            if (!imp.endsWith(ASTERISK_FORMAT)) continue;
            if (imp.startsWith(STATIC_IDENTIFIER)) continue;

            String pkg = imp.substring(0, imp.length() - 2);
            PackageNode packageNode = getPackageNode(pkg);

            if (packageNode != null && packageNode.containsClass(className)) {
                return pkg + MemberFormatter.PACKAGE_DELIMITER + className;
            }
        }

        // 4. Static imports
        for (String imp : importList) {
            if (!imp.startsWith(STATIC_IDENTIFIER)) continue;

            String actual = imp.substring(STATIC_IDENTIFIER.length());

            if (imp.endsWith(ASTERISK_FORMAT)) {
                String classFqn = actual.substring(0, actual.length() - 2);

                // classFqn = something like java.util.Map
                int lastDot = classFqn.lastIndexOf(MemberFormatter.PACKAGE_DELIMITER);
                if (lastDot == -1) continue;

                String pkg = classFqn.substring(0, lastDot);
                String clazz = classFqn.substring(lastDot + 1);

                PackageNode pkgNode = getPackageNode(pkg);
                if (pkgNode == null) continue;

                ClassNode base = pkgNode.classes.get(clazz);
                if (base != null && base.containsStatic(className)) {
                    return classFqn + MemberFormatter.INNER_CLASS_DELIMITER + className;
                }

            } else {
                // single static import
                String simple = actual.substring(actual.lastIndexOf(MemberFormatter.PACKAGE_DELIMITER) + 1);

                if (simple.equals(className)) {
                    return actual;
                }
            }
        }

        return null;
    }

    // TODO: redo separation logic, use something weird like ! separator, and allow nested class support
    // right now "a.b.C" won't see all the nested classes like "a.b.C.D" and they will be marked as external
    public String resolveClass(CompilationUnit cu, String className) {
        String packageName = cu.getPackageDeclaration()
            .map(PackageDeclaration::getName)
            .map(Name::asString)
            .orElse("")
            .replace(".", MemberFormatter.PACKAGE_DELIMITER);

        ArrayList<String> imports = new ArrayList<>(cu.getImports().size());
        for (var singleImport : cu.getImports()) {
            String importString = singleImport.getNameAsString();

            if (singleImport.isAsterisk()) {
                importString += ASTERISK_FORMAT;
            }

            if (singleImport.isStatic()) {
                importString = STATIC_IDENTIFIER + importString;
                // TODO figure out if at what point does the class start and replace said part
            } else {
                importString = importString.replace(".", MemberFormatter.PACKAGE_DELIMITER);
            }

            imports.add(importString);
        }

        return resolveClass(packageName, imports, className);
    }

    public static String resolveTypeDeclaration(TypeDeclaration<?> declaration) {
        String fullSimpleName =  MemberFormatter.fullSimpleName(declaration);
        String packageName = declaration.findCompilationUnit()
            .flatMap(CompilationUnit::getPackageDeclaration)
            .map(PackageDeclaration::getName)
            .map(Name::asString)
            .orElse("")
            .replace(".", MemberFormatter.PACKAGE_DELIMITER);

        if (packageName.isEmpty()) return fullSimpleName;

        return packageName + MemberFormatter.PACKAGE_DELIMITER + fullSimpleName;
    }
}
