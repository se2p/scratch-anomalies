package org.softevo.jadet.violations.editor;


import org.softevo.jadet.sca.Violation;
import org.softevo.jadet.sca.ViolationsList;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class is used to represent the violations list in the editor.
 *
 * @author Andrzej Wasylkowski
 */
public class ViolationsTable extends JTable {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 7765136080515737899L;


    /**
     * Creates a new violations table.
     *
     * @param editor Editor, in which the violations table is created.
     */
    public ViolationsTable(ViolationsEditor editor) {
        this.setModel(new ViolationsTableModel(editor.getViolations()));
        this.setGridColor(Color.WHITE);
        TableColumnModel columnModel = this.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(0).setMinWidth(50);
        columnModel.getColumn(1).setPreferredWidth(50);
        columnModel.getColumn(1).setMinWidth(50);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(2).setMinWidth(100);
        columnModel.getColumn(3).setPreferredWidth(200);
        columnModel.getColumn(3).setMinWidth(150);
        columnModel.getColumn(4).setPreferredWidth(200);
        columnModel.getColumn(4).setMinWidth(150);
        this.setShowGrid(false);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getSelectionModel().addListSelectionListener(this);
    }


    /**
     * This class is responsible for providing the violations data to the table.
     *
     * @author Andrzej Wasylkowski
     */
    class ViolationsTableModel extends AbstractTableModel {

        /**
         * Serial number of this class.
         */
        private static final long serialVersionUID = 6215214356060939874L;


        /**
         * List of violations that are to be provided.
         */
        private final ViolationsList violations;


        /**
         * Mapping from violations to their rank (or <code>null</code>, if
         * the rank is the same as that of the violation directly above).
         */
        private final Map<Violation, Integer> violation2rank;


        /**
         * Names of columns to be shown in the table.
         */
        private final String[] columns = {"Rank", "ID", "Defect indicator", "Type", "Duplicates"};


        /**
         * Creates a new table model that will provide the violations given
         * in the list.
         *
         * @param violations Violations to be provided.
         */
        public ViolationsTableModel(ViolationsList violations) {
            this.violations = violations;
            this.violation2rank = new HashMap<Violation, Integer>();
            int currentRank = 0;
            double lastDefectIndicator = -1.0;
            for (Violation violation : this.violations) {
                currentRank++;
                double defectIndicator = violation.getDefectIndicator();
                if (defectIndicator != lastDefectIndicator) {
                    lastDefectIndicator = defectIndicator;
                    violation2rank.put(violation, currentRank);
                } else {
                    violation2rank.put(violation, null);
                }
            }
        }


        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return this.violations.size();
        }


        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return columns.length;
        }


        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnName(int)
         */
        @Override
        public String getColumnName(int columnIndex) {
            return this.columns[columnIndex];
        }


        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }


        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int rowIndex, int columnIndex) {
            Violation violation = this.violations.get(rowIndex);
            if (this.columns[columnIndex].equalsIgnoreCase("rank")) {
                return this.violation2rank.get(violation);
            } else if (this.columns[columnIndex].equalsIgnoreCase("id")) {
                return rowIndex + 1;
            } else if (this.columns[columnIndex].equalsIgnoreCase("defect indicator")) {
                double indicatorDouble = violation.getDefectIndicator();
                BigDecimal indicatorBD = new BigDecimal(indicatorDouble);
                return indicatorBD.setScale(2, RoundingMode.HALF_EVEN);
            } else if (this.columns[columnIndex].equalsIgnoreCase("type")) {
                return violation.getType().getStringRepresentation();
            } else if (this.columns[columnIndex].equalsIgnoreCase("duplicates")) {
                List<Integer> duplicates = this.violations.getDuplicates(rowIndex + 1);
                if (duplicates.isEmpty()) {
                    return "";
                } else {
                    return duplicates.toString();
                }
            } else {
                System.err.println("Unknown column name: " +
                        this.columns[columnIndex]);
                throw new InternalError();
            }
        }
    }
}
