package gui.components;

import gui.frame.MethodFrame;
import translate.complexity.result.ComplexityMetricResult;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.translator.ComplexityTranslator;

public class MethodTableMetricsPanel extends BaseTableMetricsPanel<ComplexityEvaluator.Method> {
    private final String clazz;

    public MethodTableMetricsPanel(String clazz, ComplexityTranslator translator) {
        this.clazz = clazz;
        super(translator);
    }

    @Override
    public void onClick(String selectedFqn) {
        new MethodFrame(clazz, selectedFqn, translator);
    }

    @Override
    public ComplexityEvaluator.Method[] getEvaluators() {
        return translator.getMethodEvaluators();
    }

    @Override
    public String[] getReferences() {
        return translator.getMethodsArray(clazz);
    }

    @Override
    public ComplexityMetricResult[] evaluateReference(String reference) {
        return translator.evaluateMethod(clazz, reference).getRawMetricData();
    }
}
