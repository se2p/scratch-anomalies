package org.softevo.jadet.violations.editor;


import org.softevo.jadet.sca.ViolationsList;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;


/**
 * This is the main class of the GUI viewer/editor of violations.
 *
 * @author Andrzej Wasylkowski
 */
public class ViolationsEditor extends WindowAdapter implements Runnable {

    /**
     * File with the violations in the XML format.
     */
    private File violationsFile;


    /**
     * List of violations loaded from the violations file.
     */
    private ViolationsList violations;


    /**
     * Main frame of this editor.
     */
    private JFrame frame;


    /**
     * Menu bar of this editor.
     */
    private MenuBar menuBar;


    /**
     * Creates a new violations editor that will act on the given file.
     *
     * @param violationsFile XML file with violations.
     */
    public ViolationsEditor(File violationsFile) {
        this.violationsFile = violationsFile;
        violations = ViolationsList.readFromXML(violationsFile);
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (info.getName().equals("Nimbus")) {
                    UIManager.setLookAndFeel(info.getClassName());
                }
            }
        } catch (Exception e) {
        }
    }


    /**
     * Returns the list of violations loaded from the violations file.
     *
     * @return List of violations loaded from the violations file.
     */
    public ViolationsList getViolations() {
        return this.violations;
    }


    /**
     * Returns the menu bar that is used by the editor.
     *
     * @return Menu bar that is used by the editor.
     */
    public MenuBar getMenuBar() {
        return this.menuBar;
    }


    /**
     * This method is supposed to be called whenever some violations change.
     * This makes the editor repaint the data, as necessary.
     */
    public void violationsChanged() {
        this.frame.invalidate();
        this.frame.repaint();
    }


    /* (non-Javadoc)
     * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosing(WindowEvent e) {
        this.violations.writeXML(this.violationsFile);
    }


    /**
     * Runs the violations editor. This method exits only after the user closes
     * the editor.
     */
    public void run() {
        this.frame = createGUI();
        this.frame.pack();
        this.frame.setVisible(true);
    }


    /**
     * Creates the GUI of the editor.
     *
     * @return Main frame of the GUI.
     */
    private JFrame createGUI() {
        // create the main window
        JFrame frame = new JFrame("JADET violations: " +
                this.violationsFile.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(this);
        frame.setBackground(Color.WHITE);
        frame.setForeground(Color.WHITE);

        // set up the menu
        this.menuBar = new MenuBar();
        this.menuBar.setBackground(Color.WHITE);
        this.menuBar.setForeground(Color.WHITE);
        frame.setJMenuBar(menuBar);

        // add the tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Color.WHITE);
        StatisticsTab statistics = new StatisticsTab(this);
        statistics.setForeground(Color.WHITE);
        statistics.setBackground(Color.WHITE);
        ViolationsTab component = new ViolationsTab(this, statistics);
        component.setForeground(Color.decode("#4c97ff"));
        tabbedPane.addTab("Violations", component);
        tabbedPane.addTab("Statistics", statistics);
        frame.add(tabbedPane);

        // return the main window
        return frame;
    }
}
