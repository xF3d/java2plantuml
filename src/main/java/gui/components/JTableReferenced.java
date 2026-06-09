package gui.components;

import translate.complexity.ComplexityEvaluator;
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public abstract class JTableReferenced extends JPanel {
    private final String[] fqnReferences;
    private final JTable table;

    private final String[] columnNames;
    private final String[][] data;

    private int selectedColumns = 0;
    private boolean isDecreasing = false;

    public JTableReferenced(String[] fqnReferences, ComplexityTranslator translator) {
        super(new BorderLayout());

        int columnSize = ComplexityEvaluator.Clazz.EVALUATORS.length + 1;
        this.columnNames = new String[columnSize];
        columnNames[0] = "Class";
        ComplexityEvaluator.Clazz[] evaluators = ComplexityEvaluator.Clazz.EVALUATORS;
        for (int i = 0; i < evaluators.length; i++) {
            var evaluator = evaluators[i];
            columnNames[i + 1] = evaluator.getName();
        }

        this.fqnReferences = fqnReferences;
        this.data = new String[fqnReferences.length][columnSize];

        for (int i = 0; i < fqnReferences.length; i++) {
            var reference = fqnReferences[i];
            data[i][0] = MemberFormatter.removeSeparators(reference);

            var results = translator.evaluateClass(reference).getRawData();
            for (int j = 0; j < evaluators.length; j++) {
                data[i][j + 1] = "<html>" + results[j].getValueWithColor() + "</html>";
            }
        }

        // the same as JTable#new(Object[][], String[]) beside isCellEditable is always false
        this.table = new JTable(new AbstractTableModel() {
            public String getColumnName(int column) { return columnNames[column]; }
            public int getRowCount() { return data.length; }
            public int getColumnCount() { return columnNames.length; }
            public Object getValueAt(int row, int col) { return data[row][col]; }
            public void setValueAt(Object value, int row, int col) {
                data[row][col] = value.toString();
                fireTableCellUpdated(row, col);
            }
        });

        setupCellsWidth();
        setupClickListener();

        super.add(table.getTableHeader(), BorderLayout.NORTH);
        super.add(table, BorderLayout.CENTER);
    }

    private void setupCellsWidth() {
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
    }

    private void setupClickListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) { // double-click
                    return;
                }

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (column != 0) return;

                String value = fqnReferences[row];
                onClick(value);
            }
        });

        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }
        });
    }

    public abstract void onClick(String selectedFqn);
}
