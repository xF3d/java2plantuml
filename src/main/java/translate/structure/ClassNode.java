package translate.structure;

import translate.component.MemberFormatter;

import java.util.HashMap;
import java.util.Map;

class ClassNode {
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
