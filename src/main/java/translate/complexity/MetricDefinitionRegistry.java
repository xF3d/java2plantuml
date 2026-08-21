package translate.complexity;

import file.FileLogger;

import java.util.HashMap;

public class MetricDefinitionRegistry {
    private static final HashMap<String, MetricDefinition> registry = new HashMap<>();

    public static void register(MetricDefinition definition) {
        registry.put(definition.metric(), definition);
    }

    public static MetricDefinition get(String name) {
        var value = registry.get(name);
        if (value == null) {
            FileLogger.LOGGER.warning("Metric Definition for: " + name + " not found, using no threshold");
            return MetricDefinition.dummy(name);
        }

        return value;
    }
}
