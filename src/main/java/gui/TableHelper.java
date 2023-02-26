package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class TableHelper {
    private TableHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static void clearTable(JTable table) {
        String[] s = new String[table.getColumnCount()];
        for (int i = 0; i < table.getColumnCount(); i++) {
            s[i] = table.getColumnName(i);
        }
        table.setModel(new DefaultTableModel(s, 0));
    }

    public static void initTableRows(JTable table, int rowCount) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < rowCount; i++) {
            model.insertRow(i, new Object[]{i});
        }
        table.setModel(model);
    }

    public static JTable createTable(Object[] headers, List<Integer> editableColumns) {
        JTable table = new JTable(new DefaultTableModel(headers, 0)) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (editableColumns != null) {
                    return editableColumns.contains(column);
                }
                return false;
            }
        };
        table.setCellSelectionEnabled(false);
        table.setFocusable(false);
        return table;
    }

    public static void clearRowWithMove(JTable table, int rowIndex) {
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.setValueAt("", rowIndex, i);
        }
        for (int i = rowIndex; i < table.getRowCount() - 1; i++) {
            Object s = null;
            for (int j = 1; j < table.getColumnCount(); j++) {
                s = table.getValueAt(i + 1, j);
                table.setValueAt(s, i, j);
            }
            if (s == null) {
                break;
            }
        }
        for (int j = 1; j < table.getColumnCount(); j++) {
            table.setValueAt(null, table.getRowCount() - 1, j);
        }
    }

    public static void addOrDeleteRows(JTable table, int size) {
        if (size > table.getRowCount()) {
            for (int i = table.getRowCount(); i < size; i++) {
                ((DefaultTableModel) table.getModel()).insertRow(i, new Object[]{String.valueOf(i), 1.0});
            }
        } else {
            for (int i = table.getRowCount() - 1; i >= size; i--) {
                ((DefaultTableModel) table.getModel()).removeRow(i);
            }
        }
    }

    public static void fillTable(JTable table, ArrayList<ArrayList<String>> results) {
        initTable(table, results.size());
        for (int row = 0; row < results.size(); row++) {
            ArrayList<String> rowElement = results.get(row);
            for (int column = 1; column < rowElement.size(); column++) {
                table.setValueAt(rowElement.get(column), row, column);
            }
        }
    }

    public static void initTable(JTable table, int size) {
        clearTable(table);
        initTableRows(table, size);
    }
}
