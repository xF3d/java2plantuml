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

    public void addClassNode(String fullInnerClazz) {
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
}
