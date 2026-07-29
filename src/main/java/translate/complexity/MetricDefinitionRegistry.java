package translate.complexity;

import java.util.HashMap;

public class MetricDefinitionRegistry {
    private static final HashMap<String, MetricDefinition> registry = new HashMap<>();

    public static void register(MetricDefinition definition) {
        registry.put(definition.metric(), definition);
    }

    public static MetricDefinition get(String name) {
        var value = registry.get(name);
        if (value == null) throw new IllegalArgumentException("Definition registry could not find metric: " + name);
        return value;
    }
}
