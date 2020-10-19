package org.softevo.jadet.violations.editor;

import org.softevo.jadet.sca.*;
import org.softevo.oumextractor.modelcreator1.model.Transition;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to represent the details of the chosen violation.
 *
 * @author Andrzej Wasylkowski
 */
public class ViolationDetailsPanel extends JPanel
        implements ListSelectionListener, ActionListener, DocumentListener {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -1840756269272318803L;

    /**
     * Editor of the violations.
     */
    private final ViolationsEditor editor;
    /**
     * List of violations.
     */
    private final ViolationsList violations;
    /**
     * Mapping from violations to their rank.
     */
    private final Map<Violation, Integer> violation2rank;
    /**
     * Statistics tab.
     */
    private StatisticsTab statistics;
    /**
     * Violation to be shown or <code>null</code>.
     */
    private Violation violation;

    /**
     * Output verbosity to be used.
     */
    private OutputVerbosity outputVerbosity;

    /**
     * Creates new violation details panel.
     *
     * @param editor     Editor, in which the panel is created.
     * @param statistics Statistics tab.
     */
    public ViolationDetailsPanel(ViolationsEditor editor,
                                 StatisticsTab statistics) {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBackground(Color.WHITE);
        this.editor = editor;
        this.statistics = statistics;
        this.violations = editor.getViolations();
        this.violation2rank = new HashMap<Violation, Integer>();
        int currentRank = 0;
        int lastRank = 0;
        double lastDefectIndicator = -1.0;
        for (Violation violation : this.violations) {
            currentRank++;
            double defectIndicator = violation.getDefectIndicator();
            if (defectIndicator != lastDefectIndicator) {
                lastDefectIndicator = defectIndicator;
                lastRank = currentRank;
                violation2rank.put(violation, lastRank);
            } else {
                violation2rank.put(violation, lastRank);
            }
        }

        ViewMenu viewMenu = this.editor.getMenuBar().getViewMenu();
        viewMenu.getFullRadio().addActionListener(this);
        viewMenu.getShortRadio().addActionListener(this);
        viewMenu.getVeryShortRadio().addActionListener(this);
        if (viewMenu.getFullRadio().isSelected()) {
            this.outputVerbosity = OutputVerbosity.FULL;
        } else if (viewMenu.getShortRadio().isSelected()) {
            this.outputVerbosity = OutputVerbosity.SHORT;
        } else {
            this.outputVerbosity = OutputVerbosity.VERY_SHORT;
        }

        setNoCurrentViolation();
    }

    /* (non-Javadoc)
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        ListSelectionModel model = (ListSelectionModel) e.getSource();
        if (model.isSelectionEmpty()) {
            setNoCurrentViolation();
            return;
        } else {
            Violation violation = this.violations.get(
                    model.getMinSelectionIndex());
            setCurrentViolation(violation);
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComboBox) {
            if (e.getActionCommand().equals("type")) {
                JComboBox typeComboBox = (JComboBox) e.getSource();
                String typeName = (String) typeComboBox.getSelectedItem();

                // make sure there is something to change
                if (this.violation.getType().getStringRepresentation().equals(typeName)) {
                    return;
                }

                // get the violation type
                ViolationType type = null;
                for (ViolationType t : ViolationType.values()) {
                    if (typeName.equals(t.getStringRepresentation())) {
                        type = t;
                        break;
                    }
                }
                if (type == null) {
                    System.err.println("Unexpected violation type: " + typeName);
                    throw new InternalError();
                }

                int violationId = this.violations.indexOf(this.violation) + 1;
                List<Integer> duplicatesIds = this.violations.getDuplicates(violationId);
                if (!duplicatesIds.isEmpty()) {
                    Object options[] = {"Change all types", "Abort"};
                    int option = JOptionPane.showOptionDialog(this,
                            "Changing this type will change types of all duplicates.\n" +
                                    "Are you sure you want to do this?",
                            "Changing multiple types",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            options,
                            options[1]);
                    if (option == 0) {
                        // the user wants to proceed
                        for (int id : duplicatesIds) {
                            Violation v = this.violations.get(id - 1);
                            v.setType(type);
                        }
                    } else {
                        // the user wants to abort or closed the dialog
                        typeComboBox.setSelectedItem(this.violation.getType().getStringRepresentation());
                        setCurrentViolation(this.violation);
                        return;
                    }
                }
                this.violation.setType(type);
                this.statistics.updateTheGUI();
                this.editor.violationsChanged();
                return;
            } else if (e.getActionCommand().equals("duplicate")) {
                int violationId = this.violations.indexOf(this.violation) + 1;

                JComboBox duplicateComboBox = (JComboBox) e.getSource();
                if (duplicateComboBox.getSelectedIndex() == 0) {
                    this.violations.detachDuplicate(violationId);
                } else {
                    String duplicateName =
                            (String) duplicateComboBox.getSelectedItem();
                    int id;
                    int commaIndex = duplicateName.indexOf(',');
                    if (commaIndex == -1)
                        id = Integer.valueOf(duplicateName);
                    else
                        id = Integer.valueOf(duplicateName.substring(0, commaIndex));
                    List<Integer> duplicatesIds = this.violations.getDuplicates(violationId);
                    if (duplicatesIds.contains(id))
                        return;    // nothing to be done; nothing changed
                    if (!duplicatesIds.isEmpty()) {
                        Object options[] = {"Merge duplicates' sets", "Change duplicates set", "Abort"};
                        int option = JOptionPane.showOptionDialog(this,
                                "This violation is already marked as a duplicate of\n" +
                                        "one or more other violations. Do you want to merge\n" +
                                        "the old and the new sets of duplicates or do you want\n" +
                                        "to change the duplicates set?",
                                "Already marked as a duplicate",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                options,
                                options[2]);
                        if (option == 0) {
                            // the user wants to merge; simply continue
                        } else if (option == 1) {
                            // the user wants to change sets; detach first
                            this.violations.detachDuplicate(violationId);
                        } else {
                            // the user wants to abort or closed the dialog
                            setCurrentViolation(this.violation);
                            return;
                        }
                    }
                    if (this.violation.getType() != ViolationType.UNKNOWN ||
                            !this.violation.getDescription().isEmpty()) {
                        Violation other = this.violations.get(id - 1);
                        if (!this.violation.getType().equals(other.getType()) ||
                                !this.violation.getDescription().equals(other.getDescription())) {
                            Object options[] = {"Yes, mark as duplicate of #" + id,
                                    "No, leave as it is"};
                            int option = JOptionPane.showOptionDialog(this,
                                    "Type and/or description of this violation is different\n" +
                                            "than in the presumed original:\n" +
                                            "Type: " + this.violation.getType().getStringRepresentation() + " vs. " + other.getType().getStringRepresentation() + "\n" +
                                            "Description: " + this.violation.getDescription().toString() + " vs. " + other.getDescription().toString() + "\n" +
                                            "Are you sure you want to continue?",
                                    "Different type and/or description",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE,
                                    null,
                                    options,
                                    options[1]);
                            if (option == 0) {
                                // the user wants to proceed; continue
                            } else {
                                // the user does not want to proceed or closed the dialog
                                // handle the case if there were detached duplicates
                                if (!duplicatesIds.isEmpty()) {
                                    this.violations.setDuplicate(violationId,
                                            duplicatesIds.get(0));
                                }
                                setCurrentViolation(this.violation);
                                return;
                            }
                        }
                    }
                    this.violations.setDuplicate(violationId, id);
                }
                this.statistics.updateTheGUI();
                this.editor.violationsChanged();
                setCurrentViolation(this.violation);
                return;
            }
        } else if (e.getSource() instanceof JMenuItem) {
            if (e.getActionCommand().equals("view/full")) {
                this.outputVerbosity = OutputVerbosity.FULL;
            } else if (e.getActionCommand().equals("view/short")) {
                this.outputVerbosity = OutputVerbosity.SHORT;
            } else if (e.getActionCommand().equals("view/very_short")) {
                this.outputVerbosity = OutputVerbosity.VERY_SHORT;
            } else {
                System.err.println("Unknown action command: " +
                        e.getActionCommand());
                throw new InternalError();
            }
            if (this.violation != null) {
                this.setCurrentViolation(this.violation);
            }
        } else if (e.getSource() instanceof JButton) {
            if (e.getActionCommand().equals("show_full_graphical_violation")) {
                showGraphicalViolation();
            } else if (e.getActionCommand().equals("show_approx_graphical_violation")) {
                showGraphicalViolationApproximation();
            } else {
                System.err.println("Unknown action command: " +
                        e.getActionCommand());
                throw new InternalError();
            }
        } else {
            System.err.println("Unknown action event: " + e);
            throw new InternalError();
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
     */
    public void insertUpdate(DocumentEvent e) {
        updateDescription(e.getDocument());
    }

    /* (non-Javadoc)
     * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
     */
    public void removeUpdate(DocumentEvent e) {
        updateDescription(e.getDocument());
    }

    /* (non-Javadoc)
     * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
     */
    public void changedUpdate(DocumentEvent e) {
        updateDescription(e.getDocument());
    }

    /**
     * Updates the description of the current violation to contain the string
     * that is in the given document.
     *
     * @param document Document that contains the description of the violation.
     */
    private void updateDescription(Document document) {
        try {
            // make sure there is a change
            String description = document.getText(0,
                    document.getLength());
            this.violation.setDescription(description);
            this.editor.violationsChanged();
        } catch (BadLocationException exc) {
            exc.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Makes the details panel display the given violation.
     *
     * @param violation Violation to be displayed.
     */
    private void setCurrentViolation(Violation violation) {
        // remove all components
        this.removeAll();

        // store the violation
        this.violation = violation;

        // add a panel with information about the violation
        JPanel info = createInfoPanel(violation);
        info.setAlignmentX(LEFT_ALIGNMENT);
        this.add(info);



        // add buttons for showing the violation graphically
        JPanel buttonsPanel = new JPanel();
        this.setBackground(Color.WHITE);
        JButton fullViolationViewButton =
                new JButton("Show full graphical violation");
        fullViolationViewButton.setActionCommand("show_full_graphical_violation");
        fullViolationViewButton.setBackground(Color.decode("#4c97ff"));
        fullViolationViewButton.setForeground(Color.WHITE);
        fullViolationViewButton.addActionListener(this);
        buttonsPanel.add(fullViolationViewButton);
        buttonsPanel.setBackground(Color.WHITE);
        buttonsPanel.setMaximumSize(buttonsPanel.getPreferredSize());
        buttonsPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.add(buttonsPanel);

        // add a panel with missing constraints
        JPanel missing = createConstraintsPanel(
                "Missing sequential constraints",
                this.violation.getMissingProperties());
        missing.setBackground(Color.WHITE);
        missing.setAlignmentX(LEFT_ALIGNMENT);
        this.add(missing);

        // add a panel with present constraints
        Set<EventPair> presentConstraints = new HashSet<EventPair>(
                this.violation.getPattern().getProperties());
        presentConstraints.removeAll(this.violation.getMissingProperties());
        JPanel present = createConstraintsPanel(
                "Present sequential constraints", presentConstraints);
        present.setAlignmentX(LEFT_ALIGNMENT);
        present.setBackground(Color.WHITE);
        this.add(present);

        // add a panel with supporting methods
        JPanel supporting = new JPanel();
        supporting.setBackground(Color.WHITE);
        supporting.setForeground(Color.WHITE);
        supporting.setLayout(new BoxLayout(supporting, BoxLayout.PAGE_AXIS));
        TitledBorder border = BorderFactory.createTitledBorder(
                "Sample supporting scripts");
        supporting.setBorder(border);
        for (Method method : this.violation.getPattern().getObjects()) {
            JTextPane pane = new JTextPane();
            pane.setContentType("text/plain");
            String textRepresentation = method.getTextRepresentation(this.outputVerbosity);
            String scratchBlocksOut = getNameAtEnd(textRepresentation, "scratchblocks: ");
            pane.setText(textRepresentation.substring(0, textRepresentation.indexOf("scratchblocks: ") - 1) + "\n" + scratchBlocksOut);
            pane.setEditable(false);
            pane.setBackground(null);
            pane.setBorder(null);
            supporting.add(pane);
        }
        supporting.setMaximumSize(supporting.getPreferredSize());
        supporting.setAlignmentX(LEFT_ALIGNMENT);
        this.add(supporting);

        // repaint this panel
        this.revalidate();
        this.repaint();
    }

    /**
     * Creates a panel that shows given sequential constraints.
     *
     * @param title       Title of the panel to create.
     * @param constraints Sequential constraints to be shown in the panel.
     * @return Panel with the sequential constraints.
     */
    private JPanel createConstraintsPanel(String title,
                                          Set<EventPair> constraints) {
        // sort the constraints
        List<EventPair> sortedConstraints =
                new ArrayList<EventPair>(constraints);
        Collections.sort(sortedConstraints);

        // create the panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        TitledBorder border = BorderFactory.createTitledBorder(title);
        panel.setBorder(border);

        // add all the constraints in a loop
        for (EventPair pair : sortedConstraints) {
            JLabel label = new JLabel(
                    pair.getTextRepresentation(this.outputVerbosity));
            panel.add(label);
        }

        // finish creating the panel and return it
        panel.setMaximumSize(panel.getPreferredSize());
        return panel;
    }

    /**
     * Creates a panel that shows summary information about a violation.
     *
     * @param violation Violation to display.
     * @return Panel that shows summary information about the given violation.
     */
    private JPanel createInfoPanel(Violation violation) {
        // create the panel
        JPanel info = new JPanel(new GridBagLayout());
        info.setBackground(Color.WHITE);
        info.setForeground(Color.WHITE);
        TitledBorder infoBorder = BorderFactory.createTitledBorder("Summary");
        info.setBorder(infoBorder);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = -1;

        // add the id information
        int violationId = this.violations.indexOf(violation) + 1;
        JLabel idLabel = new JLabel("Id: ");
        JLabel idValue = new JLabel(String.valueOf(violationId));
        c.gridy++;
        c.gridx = 0;
        info.add(idLabel, c);
        c.gridx = 1;
        info.add(idValue, c);

        // add the rank information
        JLabel rankLabel = new JLabel("Rank: ");
        int rank = this.violation2rank.get(violation);
        int lowestIndexWithRank = this.violations.indexOf(violation);
        while (lowestIndexWithRank > 0 && this.violation2rank.get(this.violations.get(lowestIndexWithRank - 1)) == rank) {
            lowestIndexWithRank--;
        }
        lowestIndexWithRank++;
        int topPercentile = (100 * lowestIndexWithRank) / this.violations.size();
        if ((100 * lowestIndexWithRank) % this.violations.size() != 0) {
            topPercentile++;
        }
        JLabel rankValue = new JLabel(this.violation2rank.get(violation) +
                " (in top " + lowestIndexWithRank + " violations = " +
                topPercentile + " percentile)");
        c.gridy++;
        c.gridx = 0;
        info.add(rankLabel, c);
        c.gridx = 1;
        info.add(rankValue, c);

        // add the defect indicator information
        JLabel indicatorLabel = new JLabel("Defect indicator: ");
        BigDecimal indicator = new BigDecimal(violation.getDefectIndicator());
        String indicatorString = indicator.setScale(2, RoundingMode.HALF_EVEN).toString();
        JLabel indicatorValue = new JLabel(indicatorString);
        c.gridy++;
        c.gridx = 0;
        info.add(indicatorLabel, c);
        c.gridx = 1;
        info.add(indicatorValue, c);

        // add the support information
        JLabel supportLabel = new JLabel("Support: ");
        JLabel supportValue =
                new JLabel(String.valueOf(violation.getPattern().getSupport()));
        c.gridy++;
        c.gridx = 0;
        info.add(supportLabel, c);
        c.gridx = 1;
        info.add(supportValue, c);

        // add the confidence information
        JLabel confidenceLabel = new JLabel("Confidence: ");
        BigDecimal confidence = new BigDecimal(violation.getConfidence());
        String confidenceString =
                confidence.setScale(2, RoundingMode.HALF_EVEN).toString();
        JLabel confidenceValue = new JLabel(confidenceString);
        c.gridy++;
        c.gridx = 0;
        info.add(confidenceLabel, c);
        c.gridx = 1;
        info.add(confidenceValue, c);

        addScriptInformation(violation, info, c);

        // add the potential duplicates information
        List<Integer> potentialDuplicatesIds =
                violations.getPotentialDuplicatesIds(violationId);
        StringBuffer potentialDuplicatesStr = new StringBuffer();
        if (potentialDuplicatesIds.isEmpty()) {
            potentialDuplicatesStr.append("(none)");
        } else {
            for (int id : potentialDuplicatesIds) {
                potentialDuplicatesStr.append(id).append(", ");
            }
            int len = potentialDuplicatesStr.length();
            potentialDuplicatesStr.delete(len - 2, len);
        }
        JLabel potentialDuplicatesLabel = new JLabel("Potential duplicates: ");
        JLabel potentialDuplicatesValue = new JLabel(
                potentialDuplicatesStr.toString());
        c.gridy++;
        c.gridx = 0;
        info.add(potentialDuplicatesLabel, c);
        c.gridx = 1;
        info.add(potentialDuplicatesValue, c);

        // add the box for the duplicate
        List<List<Integer>> duplicatesGroups =
                violations.getDuplicatesGroupsIds(violationId);
        JLabel duplicateLabel = new JLabel("Duplicate of: ");
        JComboBox duplicateValue = new JComboBox();
        duplicateValue.setActionCommand("duplicate");
        duplicateValue.addItem("(none)  ");
        int index = 0;
        duplicateValue.setSelectedIndex(index);
        for (List<Integer> duplicatesGroup : duplicatesGroups) {
            StringBuffer duplicatesStr = new StringBuffer();
            for (int id : duplicatesGroup) {
                if (id != violationId)
                    duplicatesStr.append(id).append(", ");
            }
            int len = duplicatesStr.length();
            if (len == 0)
                continue;
            duplicatesStr.delete(len - 2, len);
            duplicateValue.addItem(duplicatesStr.toString());
            index++;
            if (duplicatesGroup.contains(violationId))
                duplicateValue.setSelectedIndex(index);
        }
        if (duplicateValue.getItemCount() == 1) {
            duplicateLabel.setEnabled(false);
            duplicateValue.setEnabled(false);
        }
        duplicateValue.addActionListener(this);
        c.gridy++;
        c.gridx = 0;
        info.add(duplicateLabel, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        info.add(duplicateValue, c);
        c.fill = GridBagConstraints.BOTH;

        // add the type information
        JLabel typeLabel = new JLabel("Type: ");
        JComboBox typeValue = new JComboBox();
        typeValue.setActionCommand("type");
        index = -1;
        for (ViolationType type : ViolationType.values()) {
            index++;
            typeValue.addItem(type.getStringRepresentation());
            if (this.violation.getType().equals(type)) {
                typeValue.setSelectedIndex(index);
            }
        }
        typeValue.addActionListener(this);
        c.gridy++;
        c.gridx = 0;
        info.add(typeLabel, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        info.add(typeValue, c);
        c.fill = GridBagConstraints.BOTH;

        // add scratchblocks output
        JLabel scratchBlocksLabel = new JLabel("scratchblocks output: ");
        JTextArea scratchBlocksValue = new JTextArea(6, 40);
        String textRepresentation = violation.getObject().getTextRepresentation(this.outputVerbosity);
        String scratchBlocksOut = getNameAtEnd(textRepresentation, "scratchblocks: ");
        scratchBlocksValue.setText(scratchBlocksOut);
        scratchBlocksValue.setEditable(false);
        addTextField(info, c, scratchBlocksValue, scratchBlocksLabel, scratchBlocksValue);

        // add the description
        JLabel descriptionLabel = new JLabel("Description: ");
        JTextArea descriptionValue = new JTextArea(3, 40);
        descriptionValue.setText(this.violation.getDescription());
        addTextField(info, c, descriptionValue, descriptionLabel, descriptionValue);


        // finish creating the panel and return it
        info.setMaximumSize(info.getPreferredSize());
        return info;
    }

    private void addTextField(JPanel info, GridBagConstraints c, JTextArea descriptionValue, JLabel scratchBlocksLabel, JTextArea scratchBlocksValue) {
        scratchBlocksValue.setLineWrap(true);
        scratchBlocksValue.setWrapStyleWord(true);
        scratchBlocksValue.getDocument().addDocumentListener(this);
        JScrollPane scratchblocksScroll = new JScrollPane(descriptionValue);

        c.gridy++;
        c.gridx = 0;
        info.add(scratchBlocksLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.NONE;
        info.add(scratchblocksScroll, c);
        c.fill = GridBagConstraints.BOTH;
    }

    private void addScriptInformation(Violation violation, JPanel info, GridBagConstraints c) {
        String textRepresentation = violation.getObject().getTextRepresentation(this.outputVerbosity);
        addLocationOfScript("Program: ", info, c, textRepresentation, "program: ", "actor: ");
        String scriptIdentifier = textRepresentation.contains("script: ") ? "script: " : "procedure: ";
        addLocationOfScript("Actor: ", info, c, textRepresentation, "actor: ", scriptIdentifier);
        String scriptName = getName(textRepresentation, scriptIdentifier, "scratchblocks: ");
        addLabelWithValue("Script: ", info, c, scriptName);
    }

    private void addLocationOfScript(String labelText, JPanel info, GridBagConstraints c, String textRepresentation, String start, String end) {
        String programName = getName(textRepresentation, start, end);
        addLabelWithValue(labelText, info, c, programName);
    }

    private void addLabelWithValue(String labelText, JPanel info, GridBagConstraints c, String programName) {
        JLabel programLabel = new JLabel(labelText);
        JTextPane programValue = new JTextPane();
        programValue.setContentType("text/plain");
        programValue.setText(programName);
        setDefaultSettings(programValue);
        addViolationInfo(info, c, programLabel, programValue);
    }

    private void addViolationInfo(JPanel info, GridBagConstraints c, JLabel programLabel, JTextPane programValue) {
        c.gridy++;
        c.gridx = 0;
        info.add(programLabel, c);
        c.gridx = 1;
        info.add(programValue, c);
    }

    private void setDefaultSettings(JTextPane programValue) {
        programValue.setEditable(false);
        programValue.setBackground(null);
        programValue.setBorder(null);
    }

    private static String getNameAtEnd(String name, String start) {
        return name.substring(name.indexOf(start) + start.length());
    }

    private static String getName(String name, String start, String end) {
        return name.substring(name.indexOf(start) + start.length(), name.indexOf(end));
    }

    /**
     * Makes the details panel display just a reminder to choose a violation
     * too look at.
     */
    private void setNoCurrentViolation() {
        // remove all components
        this.removeAll();

        // store the empty violation
        this.violation = null;

        // add just a label in the center
        JLabel label = new JLabel(
                "Select a violation from the violations' list",
                SwingConstants.CENTER);
        this.add(label, BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
    }

    /**
     * Creates a full graph of violation and shows it in a separate window.
     */
    private void showGraphicalViolation() {
        try {
            // get a set of missing transitions
            Set<EventPair> patternProperties =
                    this.violation.getPattern().getProperties();
            Set<EventPair> missingProperties =
                    this.violation.getMissingProperties();
            Set<Transition> missingTransitions =
                    getMissingTransitions(patternProperties, missingProperties);

            // get a graph representation of the violation
            String graph = getDotRepresentation(patternProperties,
                    missingProperties, missingTransitions);
            File graphFile = File.createTempFile("JADET", ".dot");
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(graphFile));
            out.write(graph.getBytes());
            out.close();

            // show a graph as an image
            String title = "Violation id = " +
                    (this.violations.indexOf(this.violation) + 1);
            displayViolationFromFile(title, graphFile);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Creates a reduced graph of violation and shows it in a separate window.
     */
    private void showGraphicalViolationApproximation() {
        try {
            // get reduced constraints based on the pattern and the violation
            // get a set of missing transitions
            Set<EventPair> patternProperties =
                    this.violation.getPattern().getProperties();
            Set<EventPair> missingProperties =
                    this.violation.getMissingProperties();
            Set<Transition> missingTransitions =
                    getMissingTransitions(patternProperties, missingProperties);

            Set<EventPair> reducedPattern = new HashSet<EventPair>();
            Set<EventPair> reducedViolation = new HashSet<EventPair>();
            boolean noCycles = transitivelyReduceConstraints(
                    patternProperties, reducedPattern);
            noCycles &= transitivelyReduceConstraints(missingProperties,
                    reducedViolation);
            if (!noCycles) {
                Object options[] = {"Yes, show the approximation",
                        "No, show the full representation"};
                int option = JOptionPane.showOptionDialog(this,
                        "Graphical representation of the violation contains cycles.\n" +
                                "Its approximation can be incomplete. You should use the full\n" +
                                "representation instead. Are you sure you want to continue?",
                        "Cycles in the representation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[1]);
                if (option == 0) {
                    // do nothing; the user wants to see the approximation
                } else if (option == 1) {
                    reducedPattern.addAll(patternProperties);
                    reducedViolation.addAll(missingProperties);
                } else {
                    // the user closed the dialog without choosing an option
                    return;
                }
            }

            // get a graph representation of the violation
            String graph = getDotRepresentation(reducedPattern,
                    reducedViolation, missingTransitions);
            File graphFile = File.createTempFile("JADET", ".dot");
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(graphFile));
            out.write(graph.getBytes());
            out.close();

            // show a graph as an image
            String title = "Approximate violation id = " +
                    (this.violations.indexOf(this.violation) + 1);
            displayViolationFromFile(title, graphFile);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Displays a violation from the given "dot" file.
     *
     * @param title   Title of the window.
     * @param dotFile "dot" file containing the violation.
     */
    private void displayViolationFromFile(String title, File dotFile) {
        try {
            // convert the dot file into an image
            File outputFile = File.createTempFile("JADET", ".gif");
            String cmdarray[] = {"./dot2gif", dotFile.getAbsolutePath(),
                    outputFile.getAbsolutePath()};
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmdarray);
            process.waitFor();

            // show the image in a new window
            ImageIcon image = new ImageIcon(outputFile.getAbsolutePath());
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JLabel imageLabel = new JLabel();
            imageLabel.setIcon(image);
            JScrollPane scroller = new JScrollPane(imageLabel);
            frame.add(scroller);
            frame.pack();
            frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Returns a "dot" representation of the given violation.
     *
     * @param pattern            Pattern that is being violated.
     * @param missingProperties  Properties that are missing in the violation.
     * @param missingTransitions Transitions that are only in the violation.
     * @return "dot" representation of the given violation.
     */
    private String getDotRepresentation(Set<EventPair> pattern,
                                        Set<EventPair> missingProperties, Set<Transition> missingTransitions) {
        // assign unique ids to constraints
        Map<Transition, Integer> transition2id = assignIdsToTransitions(pattern);

        // create the "dot" representation
        StringBuffer repr = new StringBuffer("digraph {\n");
        repr.append("bgcolor=\"white\";\n");
        repr.append("node [shape=\"plaintext\"];\n");
        for (Transition transition : transition2id.keySet()) {
            repr.append("\"").append(transition2id.get(transition)).append("\"");
            repr.append(" [label=\"");
            switch (this.outputVerbosity) {
                case FULL:
                    repr.append(transition.getLongEventString());
                    break;
                case SHORT:
                    repr.append(transition.getShortEventString());
                    break;
                case VERY_SHORT:
                    repr.append(transition.getVeryShortEventString());
                    break;
            }
            repr.append("\"");
            if (missingTransitions.contains(transition)) {
                repr.append(",fontcolor=red");
            } else {
                repr.append(",fontcolor=blue");
            }
            repr.append("];\n");
        }
        for (EventPair pair : pattern) {
            if (missingProperties.contains(pair)) {
                continue;
            }
            int left = transition2id.get(pair.getLeft());
            int right = transition2id.get(pair.getRight());
            repr.append("\"").append(left).append("\"");
            repr.append("->");
            repr.append("\"").append(right).append("\"");
            repr.append(" [color=blue, style=bold]");
            repr.append(";\n");
        }
        for (EventPair pair : missingProperties) {
            int left = transition2id.get(pair.getLeft());
            int right = transition2id.get(pair.getRight());
            repr.append("\"").append(left).append("\"");
            repr.append("->");
            repr.append("\"").append(right).append("\"");
            if (!missingTransitions.contains(pair.getLeft()) &&
                    !missingTransitions.contains(pair.getRight())) {
                repr.append(" [color=red]");
            } else {
                repr.append(" [color=pink]");
            }
            repr.append(";\n");
        }
        repr.append("}");
        return repr.toString();
    }

    /**
     * Finds a set of missing transitions (i.e., those that occur only in
     * the missing properties).
     *
     * @param patternProperties Properties that occur in the pattern.
     * @param missingProperties Properties that are missing in the violation.
     * @return Set of missing transitions.
     */
    private Set<Transition> getMissingTransitions(
            Set<EventPair> patternProperties,
            Set<EventPair> missingProperties) {
        Set<EventPair> presentProperties =
                new HashSet<EventPair>(patternProperties);
        presentProperties.removeAll(missingProperties);
        Set<Transition> missingTransitions = new HashSet<Transition>();
        for (EventPair property : missingProperties) {
            missingTransitions.add(property.getLeft());
            missingTransitions.add(property.getRight());
        }
        for (EventPair property : presentProperties) {
            missingTransitions.remove(property.getLeft());
            missingTransitions.remove(property.getRight());
        }
        return missingTransitions;
    }

    /**
     * Does a transitive reduction on the given set of constraints.
     *
     * @param constraints        Set of constraints to reduce.
     * @param reducedConstraints Output parameter with transitive reduction
     *                           of the set of constraints given as the first
     *                           parameter.
     * @return <code>true</code> if there were no cycles; <code>false</code>
     * otherwise.
     */
    private boolean transitivelyReduceConstraints(Set<EventPair> constraints,
                                                  Set<EventPair> reducedConstraints) {
        try {
            boolean noCycles = true;

            // map transitions to ids and vice versa
            Map<Transition, Integer> transition2id = assignIdsToTransitions(constraints);
            Map<Integer, Transition> id2transition = new HashMap<Integer, Transition>();
            for (Transition transition : transition2id.keySet()) {
                Integer id = transition2id.get(transition);
                id2transition.put(id, transition);
            }

            // create the graph file with constraints
            StringBuffer graph = new StringBuffer();
            graph.append("digraph {\n");
            for (EventPair pair : constraints) {
                int left = transition2id.get(pair.getLeft());
                int right = transition2id.get(pair.getRight());
                if (left == right) {
                    return false;
                }
                graph.append(left).append("->").append(right).append(";\n");
            }
            graph.append("}\n");

            // write the graph file
            File inputFile = File.createTempFile("JADET", ".dot");
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(inputFile));
            out.write(graph.toString().getBytes());
            out.close();

            // use tred to compute the transitive reduction
            String cmdarray[] = {"tred", inputFile.getAbsolutePath()};
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmdarray);
            process.waitFor();
            if (process.getErrorStream().available() > 0) {
                noCycles = false;
            }

            // read the constraints from the output graph
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            Pattern edgePattern = Pattern.compile("\\s*\"?+(\\d+)\"?+\\s*->\\s*\"?+(\\d+)\"?+.*");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = edgePattern.matcher(line);
                if (matcher.matches()) {
                    int left = Integer.valueOf(matcher.group(1));
                    int right = Integer.valueOf(matcher.group(2));
                    Transition transitionLeft = id2transition.get(left);
                    Transition transitionRight = id2transition.get(right);
                    EventPair pair = EventPair.get(transitionLeft,
                            transitionRight);
                    reducedConstraints.add(pair);
                }
            }
            reader.close();
            return noCycles;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Assigns unique id to each transition from the given set of events.
     *
     * @param events Events to consider.
     * @return Mapping from transition to id.
     */
    private Map<Transition, Integer> assignIdsToTransitions(Set<EventPair> events) {
        Map<Transition, Integer> transition2id = new HashMap<Transition, Integer>();
        for (EventPair pair : events) {
            Transition left = pair.getLeft();
            Transition right = pair.getRight();
            if (!transition2id.containsKey(left)) {
                int id = transition2id.size() + 1;
                transition2id.put(left, id);
            }
            if (!transition2id.containsKey(right)) {
                int id = transition2id.size() + 1;
                transition2id.put(right, id);
            }
        }
        return transition2id;
    }
}
