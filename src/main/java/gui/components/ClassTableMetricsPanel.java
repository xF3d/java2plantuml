package gui.components;

import gui.frame.ClassFrame;
import translate.complexity.ComplexityMetricResult;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

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
    public ComplexityMetricResult[] evaluateReference(String reference) {
        return translator.evaluateClass(reference).getRawMetricData();
    }
}
