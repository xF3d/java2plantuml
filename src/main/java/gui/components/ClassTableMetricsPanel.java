package gui.components;

import gui.frame.ClassFrame;
import translate.complexity.result.BoundedResult;
import translate.complexity.result.ComplexityMetricResult;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.translator.ComplexityTranslator;

public class ClassTableMetricsPanel extends BaseTableMetricsPanel<ComplexityEvaluator.Clazz> {
    public ClassTableMetricsPanel(ComplexityTranslator translator) {
        super(translator);
    }

    @Override
    public void onClick(String selectedFqn) {
        new ClassFrame(selectedFqn, translator);
    }

    @Override
    public ComplexityEvaluator.Clazz[] getEvaluators() {
        return translator.getClassEvaluators();
    }

    @Override
    public String[] getReferences() {
        return translator.getClassesArray();
    }

    @Override
    public BoundedResult[] evaluateReference(String reference) {
        return translator.evaluateClass(reference).getRawMetricData();
    }
}
