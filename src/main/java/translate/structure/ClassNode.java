package translate.structure;

import com.google.common.base.Preconditions;
import translate.component.MemberFormatter;

import java.util.HashMap;
import java.util.Map;

class ClassNode {
    String name;
    PackageNode location;
    Map<String, ClassNode> innerClasses = new HashMap<>();

    ClassNode(PackageNode location, String name) {
        this.location = location;
        this.name = name;
    }

    /**
     *
     * @param inners string containing the full scoped name, ["ClassName", "Inner", "Inner2"], first argument should the class name itself
     */
    public void addInner(String[] inners) {
        // first element is just the name itself, so we can skip
        Preconditions.checkArgument(inners.length > 0);
        Preconditions.checkArgument(name.equals(inners[0]));

        ClassNode current = this;
        for (int i = 1; i < inners.length; i++) {
            current.innerClasses.putIfAbsent(inners[i], new ClassNode(this.location, inners[i]));
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
