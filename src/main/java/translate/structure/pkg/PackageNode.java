package translate.structure.pkg;

import translate.component.formatter.MemberFormatter;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

class PackageNode {
    final String name;
    final PackageNode parent;
    Map<String, PackageNode> children = new ConcurrentHashMap<>();
    Map<String, ClassNode> classes = new ConcurrentHashMap<>();

    PackageNode(String name, PackageNode parent) {
        this.name = name;
        this.parent = parent;
    }

    static PackageNode root() {
        return new PackageNode("", null);
    }

    public void addClassNode(String fullInnerClazz) {
        String[] split = MemberFormatter.INNER_CLASS_PATTERN.split(fullInnerClazz);

        ClassNode foundBaseClass = classes.get(split[0]);
        if (foundBaseClass == null) {
            ClassNode classNode = new ClassNode(this, split[0]);
            classNode.addInner(split);
            classes.put(split[0], classNode);
        } else {
            foundBaseClass.addInner(split);
        }
    }

    public boolean containsClassStatic(String fullInnerClazz) {
        String[] split = MemberFormatter.INNER_CLASS_PATTERN.split(fullInnerClazz);
        ClassNode foundBaseClass = classes.get(split[0]);

        if (foundBaseClass == null) return false;

        return foundBaseClass.containsStatic(fullInnerClazz);
    }

    public PackageNode getPackageNode(String packageName) {
        if (packageName == null || packageName.isEmpty()) return this;

        String[] parts = MemberFormatter.PACKAGE_PATTERN.split(packageName);
        PackageNode current = this;

        for (String part : parts) {
            current = current.children.get(part);
            if (current == null) return null;
        }

        return current;
    }

    /**
     *
     * @param packageName should use MemberFormatter#PACKAGE_DELIMITER  only
     * @return
     */
    public ClassNode getStaticPackageNode(String packageName) {
        if (packageName == null || packageName.isEmpty()) return null;

        String[] parts = MemberFormatter.PACKAGE_PATTERN.split(packageName);

        PackageNode currentPackage = this;
        int i;
        for (i = 0; i < parts.length; i++) {
            String part = parts[i];
            PackageNode next = currentPackage.children.get(part);
            if (next == null) break;
            currentPackage = next;
        }

        ClassNode currentClass = null;
        for (; i < parts.length; i++) {
            String part = parts[i];

            if (currentClass == null) {
                currentClass = currentPackage.classes.get(part);
            } else {
                currentClass = currentClass.innerClasses.get(part);
            }

            if (currentClass == null) return null;
        }

        return currentClass;
    }

    public ClassNode getRaw(String packageName) {
        if (packageName == null || packageName.isEmpty()) return null;

        String[] parts = packageName.split("\\.");

        PackageNode currentPackage = this;
        int i;
        for (i = 0; i < parts.length; i++) {
            String part = parts[i];
            PackageNode next = currentPackage.children.get(part);
            if (next == null) break;
            currentPackage = next;
        }

        ClassNode currentClass = null;
        for (; i < parts.length; i++) {
            String part = parts[i];

            if (currentClass == null) {
                currentClass = currentPackage.classes.get(part);
            } else {
                currentClass = currentClass.innerClasses.get(part);
            }

            if (currentClass == null) return null;
        }

        return currentClass;
    }

    public boolean containsRaw(String packageName) {
        return getRaw(packageName) != null;
    }

    public String mapRaw(String packageName) {
        if (packageName == null || packageName.isEmpty()) return null;

        String[] parts = packageName.split("\\.");

        PackageNode currentPackage = this;
        ArrayList<String> packageParts = new ArrayList<>();
        int i;

        for (i = 0; i < parts.length; i++) {
            String part = parts[i];
            PackageNode next = currentPackage.children.get(part);
            if (next == null) break;
            currentPackage = next;
            packageParts.add(next.name);
        }

        ClassNode currentClass = null;
        ArrayList<String> classParts = new ArrayList<>();

        for (; i < parts.length; i++) {
            String part = parts[i];

            if (currentClass == null) {
                currentClass = currentPackage.classes.get(part);
            } else {
                currentClass = currentClass.innerClasses.get(part);
            }


            if (currentClass == null) {
                break;
            } else {
                classParts.add(currentClass.name);
            }
        }

        return String.join(MemberFormatter.PACKAGE_DELIMITER, packageParts) +
                MemberFormatter.PACKAGE_DELIMITER +
                String.join(MemberFormatter.INNER_CLASS_DELIMITER, classParts);
    }
}
