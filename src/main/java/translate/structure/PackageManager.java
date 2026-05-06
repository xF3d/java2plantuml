package translate.structure;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import translate.component.MemberFormatter;

import java.util.ArrayList;
import java.util.List;

public class PackageManager {

    private static final String ASTERISK_FORMAT = MemberFormatter.PACKAGE_DELIMITER + "*";
    private static final String STATIC_IDENTIFIER = "#";

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
            String importedClass = imp.substring(lastDot + MemberFormatter.PACKAGE_DELIMITER.length());

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

            String pkg = imp.substring(0, imp.length() - ASTERISK_FORMAT.length());
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
            }

            importString = importString.replace(".", MemberFormatter.PACKAGE_DELIMITER);

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
