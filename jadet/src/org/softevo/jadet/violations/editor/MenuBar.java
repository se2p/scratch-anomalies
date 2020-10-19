package org.softevo.jadet.violations.editor;


import javax.swing.*;
import java.awt.*;


/**
 * This class is used to create the menu for the violations editor.
 *
 * @author Andrzej Wasylkowski
 */
public class MenuBar extends JMenuBar {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -4620458861005756660L;


    /**
     * The "View" menu.
     */
    private ViewMenu view;


    /**
     * Creates the menu bar.
     */
    public MenuBar() {
        // build the "View" menu
        this.view = new ViewMenu();
        this.view.setBackground(Color.WHITE);
        this.add(this.view);
    }


    /**
     * Returns the view menu.
     *
     * @return The view menu.
     */
    public ViewMenu getViewMenu() {
        return this.view;
    }
}
