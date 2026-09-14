package gui.components;

import translate.complexity.result.ComplexityMetricResult;
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

public abstract class BaseTableMetricsPanel<T extends ComplexityEvaluator> extends JPanel {
    private static final Comparator<String> HTML_DOUBLE_COMPARATOR =
            Comparator.comparingDouble(BaseTableMetricsPanel::extractDouble);

    protected final JTable table;

    protected int selectedColumns = 0;
    protected SortOrder sortOrder = SortOrder.UNSORTED;

    protected final String[] fqnReferences;
    protected final String[] columnNames;
    protected final String[][] data;

    protected final ComplexityTranslator translator;

    public BaseTableMetricsPanel(ComplexityTranslator translator) {
        super(new BorderLayout());
        this.translator = translator;

        int columnSize = getEvaluators().length + 1;
        this.columnNames = new String[columnSize];
        columnNames[0] = "Class";
        ComplexityEvaluator[] evaluators = getEvaluators();
        for (int i = 0; i < evaluators.length; i++) {
            var evaluator = evaluators[i];
            columnNames[i + 1] = evaluator.getDefinition().metric();
        }

        this.fqnReferences = getReferences();
        this.data = new String[fqnReferences.length][columnSize];

        for (int i = 0; i < fqnReferences.length; i++) {
            var reference = fqnReferences[i];
            data[i][0] = MemberFormatter.removeSeparators(reference);

            var results = evaluateReference(reference);
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

                Point p = e.getPoint();

                int viewRow = table.rowAtPoint(p);
                int modelRow = table.convertRowIndexToModel(viewRow);
                int column = table.columnAtPoint(p);
                if (column != 0) return;

                String value = fqnReferences[modelRow];
                onClick(value);
            }
        });


        var sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        for (int i = 1; i < columnNames.length; i++) {
            sorter.setComparator(i, HTML_DOUBLE_COMPARATOR);
        }

        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column <= 0) return;

                if (selectedColumns == column) {
                    sortOrder = SortOrder.values()[(sortOrder.ordinal() + 1) % SortOrder.values().length];
                } else {
                    selectedColumns = column;
                    sortOrder = SortOrder.DESCENDING;
                }

                sorter.setSortKeys(List.of(new RowSorter.SortKey(column, sortOrder)));
                sorter.sort();
            }
        });
    }

    public abstract void onClick(String selectedFqn);

    public abstract T[] getEvaluators();

    public abstract String[] getReferences();

    public abstract ComplexityMetricResult[] evaluateReference(String reference);

    private static double extractDouble(String html) {
        // remove HTML tags
        String text = html.replaceAll("<[^>]*>", "").trim();

        // if other characters are present, keep only the number
        text = text.replaceAll("[^0-9+\\-.,Ee]", "");

        return Double.parseDouble(text.replace(',', '.'));
    }
}
