package translate.structure;

import translate.component.MemberFormatter;

import java.util.HashMap;
import java.util.Map;

class PackageNode {
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
