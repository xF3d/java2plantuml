package translate.structure;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import translate.component.MemberFormatter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PackageManager {

    private final PackageNode root = PackageNode.root();

    public void addClass(String fqn) {
        String[] parts = fqn.split("\\" + MemberFormatter.PACKAGE_DELIMITER);
        PackageNode current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            PackageNode before = current;
            current.children.putIfAbsent(parts[i], new PackageNode(parts[i], before));
            current = current.children.get(parts[i]);
        }

        current.addClassNode(parts[parts.length - 1]);
    }

    /**
     *
     * @param packageLocation where does the class resolution start, to scan local classes
     * @param importList list of all imports
     * @param className full class name with proper inner classes formatting
     * @return fqn of the className
     */
    public String resolveClass(String packageLocation, List<PackageImport> importList, String className) {
        // 0. Filter lists into appropriate enviroment
        LinkedList<PackageImport> explicit = new LinkedList<>();
        LinkedList<PackageImport> wildcards = new LinkedList<>();
        LinkedList<PackageImport> staticExplicit = new LinkedList<>();
        LinkedList<PackageImport> staticWildcards = new LinkedList<>();

        for (PackageImport imp : importList) {
            boolean isStatic = imp.isStatic();
            boolean isAsterisk = imp.isAsterisk();

            if (!isAsterisk && !isStatic) explicit.add(imp);
            if (isAsterisk && !isStatic) wildcards.add(imp);
            if (!isAsterisk && isStatic) staticExplicit.add(imp);
            if (isAsterisk && isStatic) staticWildcards.add(imp);
        }


        // 1. Explicit (non-static, non-wildcard) imports
        for (PackageImport imp : explicit) {
            int lastDot = imp.fqn().lastIndexOf(MemberFormatter.PACKAGE_DELIMITER);

            if (lastDot == -1) continue;
            String pkg = imp.fqn().substring(0, lastDot);

            PackageNode importPkg = root.getPackageNode(pkg);
            if (importPkg != null && importPkg.containsClassStatic(className)) {
                return pkg + MemberFormatter.PACKAGE_DELIMITER + className;
            }
        }

        // 2. Same package
        PackageNode currentPkg = root.getPackageNode(packageLocation);
        if (currentPkg != null && currentPkg.containsClassStatic(className)) {
            return packageLocation + MemberFormatter.PACKAGE_DELIMITER + className;
        }

        // 3. Wildcard imports
        for (PackageImport imp : wildcards) {
            String pkg = imp.fqn();
            PackageNode packageNode = root.getPackageNode(pkg);

            if (packageNode != null && packageNode.containsClassStatic(className)) {
                return pkg + MemberFormatter.PACKAGE_DELIMITER + className;
            }
        }

        // 4. Static imports
        for (PackageImport imp : staticExplicit) {
            String classFqn = imp.fqn();
            ClassNode node = root.getStaticPackageNode(classFqn);
            if (node != null) {
                String packageName = node.location.name;
                String classSection = classFqn.substring(packageName.length() + MemberFormatter.PACKAGE_DELIMITER.length());
                return packageName + MemberFormatter.PACKAGE_DELIMITER + classSection.replace(MemberFormatter.PACKAGE_DELIMITER, MemberFormatter.INNER_CLASS_DELIMITER);
            }
        }

        // 5. Static wildcard imports
        for (PackageImport imp : staticWildcards) {
            String classFqn = imp.fqn() + MemberFormatter.PACKAGE_DELIMITER + className;
            ClassNode node = root.getStaticPackageNode(classFqn);
            if (node != null) {
                String packageName = node.location.name;
                String classSection = classFqn.substring(packageName.length() + MemberFormatter.PACKAGE_DELIMITER.length());
                return packageName + MemberFormatter.PACKAGE_DELIMITER + classSection.replace(MemberFormatter.PACKAGE_DELIMITER, MemberFormatter.INNER_CLASS_DELIMITER);
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

        ArrayList<PackageImport> imports = new ArrayList<>(cu.getImports().size());
        for (ImportDeclaration singleImport : cu.getImports()) {
            String importString = singleImport.getNameAsString();

            importString = importString.replace(".", MemberFormatter.PACKAGE_DELIMITER);

            imports.add(new PackageImport(importString, singleImport.isAsterisk(), singleImport.isStatic()));
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
