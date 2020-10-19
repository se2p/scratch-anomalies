package org.softevo.jadet.violations.editor;


import javax.swing.*;
import java.awt.*;


/**
 * This class is used to create the "View" menu.
 *
 * @author Andrzej Wasylkowski
 */
public class ViewMenu extends JMenu {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 2900351277532086209L;


    /**
     * Radio menu item for full method names.
     */
    private JRadioButtonMenuItem fullRadio;


    /**
     * Radio menu item for method names without packages in signatures.
     */
    private JRadioButtonMenuItem shortRadio;


    /**
     * Radio menu item for method names without packages.
     */
    private JRadioButtonMenuItem veryShortRadio;


    /**
     * Creates a view menu.
     */
    public ViewMenu() {
        super("View");

        this.setBackground(Color.decode("#4C97FF"));
        ButtonGroup group = new ButtonGroup();

        // create the radio menu item for full method names
        this.fullRadio = new JRadioButtonMenuItem("Full method names");
        this.fullRadio.setActionCommand("view/full");
        group.add(this.fullRadio);
        this.add(this.fullRadio);

        // create the radio menu item for short method names
        this.shortRadio = new JRadioButtonMenuItem("No packages in signatures");
        this.shortRadio.setActionCommand("view/short");
        group.add(this.shortRadio);
        this.add(this.shortRadio);

        // create the radio menu item for very short method names
        this.veryShortRadio = new JRadioButtonMenuItem("No packages");
        this.veryShortRadio.setActionCommand("view/very_short");
        this.veryShortRadio.setSelected(true);
        group.add(this.veryShortRadio);
        this.add(this.veryShortRadio);
    }


    /**
     * Returns the radio menu item for full method names.
     *
     * @return Radio menu item for full method names.
     */
    public JRadioButtonMenuItem getFullRadio() {
        return this.fullRadio;
    }


    /**
     * Returns the radio menu item for method names without packages in signatures.
     *
     * @return Radio menu item for method names without packages in signatures.
     */
    public JRadioButtonMenuItem getShortRadio() {
        return this.shortRadio;
    }


    /**
     * Returns the radio menu item for method names without packages.
     *
     * @return Radio menu item for method names without packages.
     */
    public JRadioButtonMenuItem getVeryShortRadio() {
        return this.veryShortRadio;
    }
}
