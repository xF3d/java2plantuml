package gui.frame;

import gui.components.JListReferenced;
import gui.components.ReferenceController;
import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import org.knowm.xchart.XChartPanel;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class SystemStatus extends JFrame {
    private final RadarChart chart;
    private final JPanel content = new JPanel();
    private final ComplexityTranslator translator;

    public SystemStatus(ComplexityTranslator translator) {
        super("System Status");
        this.translator = translator;
        this.chart = new RadarChartBuilder()
            .width(800)
            .height(500)
            .build();

        ComplexityMetricResult[] results = translator.evaluateSystem();

        String[] names = new String[results.length];
        double[] values = new double[results.length];

        for (int i = 0; i < results.length; i++) {
            names[i] = results[i].getName();
            values[i] = results[i].normalize();
        }

        this.chart.setRadiiLabels(names);
        this.chart.addSeries("System", values);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        var scrollPanel = new JScrollPane(content);
        var chartPanel = new XChartPanel<>(chart);

        this.content.add(chartPanel);
        this.content.add(new ReferenceController(
            "<html><p>Classes in the system</p> <br></html>",
            translator.getClassJList(),
            (unused) -> getClassList(),
            (unused) -> getClassTable()
        ));

        this.setContentPane(scrollPanel);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        FrameManager.addFrame(this);
    }


    private JListReferenced jclassList = null;
    private JListReferenced getClassList() {
        if (jclassList != null) return jclassList;

        jclassList = new JListReferenced(translator.getClassJList()) {
            @Override
            public void onClick(String selectedItem) {
                new ClassFrame(selectedItem, translator);
            }
        };
        jclassList.setAlignmentX(Component.LEFT_ALIGNMENT);

        return jclassList;
    }

    private JPanel jclassTable = null;
    private JPanel getClassTable() {
        if (jclassTable != null) return jclassTable;

        int columnSize = ComplexityEvaluator.Clazz.EVALUATORS.length + 1;
        String[] columnNames = new String[columnSize];
        columnNames[0] = "Class";
        ComplexityEvaluator.Clazz[] evaluators = ComplexityEvaluator.Clazz.EVALUATORS;
        for (int i = 0; i < evaluators.length; i++) {
            var evaluator = evaluators[i];
            columnNames[i + 1] = evaluator.getName();
        }

        String[] references = translator.getClassJList();
        String[][] data = new String[references.length][columnSize];

        for (int i = 0; i < references.length; i++) {
            var reference = references[i];
            data[i][0] = MemberFormatter.removeSeparators(reference);

            var results = translator.evaluateClass(reference);
            for (int j = 0; j < evaluators.length; j++) {
                data[i][j + 1] = "<html>" + results[j].getValueWithColor() + "</html>";
            }
        }

        // the same as JTable#new(Object[][], String[]) beside isCellEditable is always false
        JTable table = new JTable(new AbstractTableModel() {
            public String getColumnName(int column) { return columnNames[column]; }
            public int getRowCount() { return data.length; }
            public int getColumnCount() { return columnNames.length; }
            public Object getValueAt(int row, int col) { return data[row][col]; }
            public void setValueAt(Object value, int row, int col) {
                data[row][col] = value.toString();
                fireTableCellUpdated(row, col);
            }
        });

        TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = -1;
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width +1 , width);
            }

            if(width > 300) width=300;

            columnModel.getColumn(column).setPreferredWidth(width);
        }

        jclassTable = new JPanel(new BorderLayout());
        jclassTable.add(table.getTableHeader(), BorderLayout.NORTH);
        jclassTable.add(table, BorderLayout.CENTER);
        return jclassTable;
    }
}
