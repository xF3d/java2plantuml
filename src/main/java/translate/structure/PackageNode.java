package translate.structure;

import translate.component.MemberFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        String[] split = fullInnerClazz.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);

        ClassNode foundBaseClass = classes.get(split[0]);
        if (foundBaseClass == null) {
            ClassNode classNode = new ClassNode(this, split[0]);
            classNode.addInner(split);
            classes.put(split[0], classNode);
        } else {
            foundBaseClass.addInner(split);
        }
    }

    public ClassNode getClassNode(String fullInnerClazz) {
        String[] split = fullInnerClazz.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);

        ClassNode foundBaseClass = classes.get(split[0]);
        for (int i = 1; i < split.length && foundBaseClass != null; i++) {
            foundBaseClass = foundBaseClass.innerClasses.get(split[i]);
        }

        return foundBaseClass;
    }

    public boolean containsClassStatic(String fullInnerClazz) {
        String[] split = fullInnerClazz.split("\\" + MemberFormatter.INNER_CLASS_DELIMITER);
        ClassNode foundBaseClass = classes.get(split[0]);

        if (foundBaseClass == null) return false;

        return foundBaseClass.containsStatic(fullInnerClazz);
    }

    public PackageNode getPackageNode(String packageName) {
        if (packageName == null || packageName.isEmpty()) return this;

        String[] parts = packageName.split("\\" + MemberFormatter.PACKAGE_DELIMITER);
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

        String[] parts = packageName.split("\\" + MemberFormatter.PACKAGE_DELIMITER);

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

    /*
    public String fqn() {
        ArrayList<PackageNode> packageNodeList = new ArrayList<>();
        PackageNode current = parent;

        while (current != null) {

            current = current.parent;
        }

    }*/
}
