package translate.component;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SetTranslatingComponent<T extends Node> {
    protected final Set<T> set;
    protected final Class<T> type;

    protected SetTranslatingComponent(Class<T> type) {
        this.type = type;
        this.set = new HashSet<>();
    }

    // needs custom implementation for class & interface
    public void safeAdd(Object object) {
        if (type.isInstance(object)) {
            set.add(type.cast(object));
        }
    }

    public void add(T... elements) {
        set.addAll(Arrays.asList(elements));
    }

    public void add(Collection<? extends T> collection) {
        set.addAll(collection);
    }

    public Class<T> type() {
        return type;
    }

    public Map<String, List<String>> write() {
        HashMap<String, List<String>> map = new HashMap<>();
        for (var element : set) {
            String packageName = element.findCompilationUnit()
                    .flatMap(CompilationUnit::getPackageDeclaration)
                    .map(PackageDeclaration::getName)
                    .map(Name::asString)
                    .orElse("");

            String clazz = writeComponent(element);
            map.computeIfAbsent(packageName, (key) -> new ArrayList<>());
            map.computeIfPresent(packageName, (k, v) -> {
                v.add(clazz);
                return v;
            });
        }

        return map;
    }

    public abstract String writeComponent(T element);
}
