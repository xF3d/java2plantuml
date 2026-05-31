package translate.component;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Name;
import lombok.Getter;
import translate.component.formatter.MemberFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SetTranslatingComponent<T extends Node> {
    @Getter
    protected final Set<T> set;
    protected final Class<T> type;

    protected SetTranslatingComponent(Class<T> type) {
        this.type = type;
        this.set = new HashSet<>();
    }

    // needs custom implementation for class & interface
    public void safeAdd(Node object) {
        if (type.isInstance(object)) {
            set.add(type.cast(object));
        }
    }

    /*
    public void add(T... elements) {
        set.addAll(Arrays.asList(elements));
    }

    public void add(Collection<? extends T> collection) {
        set.addAll(collection);
    }*/

    public Class<T> type() {
        return type;
    }

    public UmlPackageEntry writeUML(MemberFormatter formatter) {
        ArrayList<String> associations = new ArrayList<>();
        HashMap<String, List<String>> packageMap = new HashMap<>();
        for (var element : set) {
            String packageName = element.findCompilationUnit()
                    .flatMap(CompilationUnit::getPackageDeclaration)
                    .map(PackageDeclaration::getName)
                    .map(Name::asString)
                    .orElse("")
                    .replace(".", MemberFormatter.PACKAGE_DELIMITER);

            var entry = writeComponentUML(element, formatter);
            packageMap.computeIfAbsent(packageName, (key) -> new ArrayList<>());
            packageMap.computeIfPresent(packageName, (k, v) -> {
                v.add(entry.classDefinition());
                return v;
            });
            associations.add(entry.associations());
        }

        return new UmlPackageEntry(packageMap, associations);
    }

    public abstract UmlEntry writeComponentUML(T element, MemberFormatter formatter);
}
