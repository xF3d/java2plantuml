package translate.complexity;

public enum MetricTarget {
    HIGHER, LOWER, NONE;

    public double normalize(double value, double min, double max) {
        if (this == NONE) return value;

        if (value < min || max < value) {
            return 0.0;
        }

        double normalized = (value - min) / (max - min);
        if (this == HIGHER) return normalized;
        else return 1.0 - normalized;
    }

    public double normalizeInt(double value, int min, int max) {
        if (this == NONE) return value;

        if (value < min || max < value) {
            return 0.0;
        }

        double normalized = (value - min) / (max - min);
        if (this == HIGHER) return normalized;
        else return 1.0 - normalized;
    }
}
