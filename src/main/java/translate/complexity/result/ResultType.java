package translate.complexity.result;

import translate.complexity.MetricDefinition;

public enum ResultType {
    INT {
        @Override
        public BoundedResult create(MetricDefinition def, double value) {
            return new IntMetricResult(def, value);
        }
    },
    DOUBLE {
        @Override
        public BoundedResult create(MetricDefinition def, double value) {
            return new ComplexityMetricResult(def, value);
        }
    };

    public abstract BoundedResult create(MetricDefinition def, double value);
}
