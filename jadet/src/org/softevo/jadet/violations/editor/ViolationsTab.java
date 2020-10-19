package org.softevo.jadet.violations.editor;


import javax.swing.*;
import java.awt.*;


/**
 * This class is used to represent the violations tab in the violations editor.
 *
 * @author Andrzej Wasylkowski
 */
public class ViolationsTab extends JPanel {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -825695587946819679L;


    /**
     * Table showing violations.
     */
    private final ViolationsTable violationsTable;


    /**
     * Panel showing details about the selected violation.
     */
    private final ViolationDetailsPanel violationsDetails;


    /**
     * Creates the overview tab of the violations editor.
     *
     * @param editor     Editor, in which the panel is created.
     * @param statistics Statistics tab.
     */
    public ViolationsTab(ViolationsEditor editor, StatisticsTab statistics) {
        super(new BorderLayout());
        this.setBackground(Color.WHITE);

        // create components
        this.violationsTable = new ViolationsTable(editor);
        this.violationsDetails = new ViolationDetailsPanel(editor, statistics);
        this.setForeground(Color.WHITE);

        // create a split pane
        JSplitPane splitPane = new JSplitPane();
        splitPane.setBackground(Color.WHITE);
        JScrollPane comp = new JScrollPane(this.violationsTable);
        comp.setBackground(Color.WHITE);
        splitPane.setLeftComponent(comp);
        JScrollPane comp1 = new JScrollPane(this.violationsDetails);
        comp1.setBackground(Color.WHITE);
        comp1.getVerticalScrollBar().setUnitIncrement(16);

        splitPane.setRightComponent(comp1);
        splitPane.getRightComponent().setPreferredSize(new Dimension(1000, 800));
        this.add(splitPane, BorderLayout.CENTER);

        // set the right pane as a listener for selections in the left pane
        this.violationsTable.getSelectionModel().addListSelectionListener(
                this.violationsDetails);
    }
}
