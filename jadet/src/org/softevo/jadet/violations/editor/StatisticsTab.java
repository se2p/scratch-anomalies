package org.softevo.jadet.violations.editor;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.softevo.jadet.sca.ViolationType;
import org.softevo.jadet.sca.ViolationsList;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class is used to represent the statistics tab in the violations editor.
 *
 * @author Andrzej Wasylkowski
 */
public class StatisticsTab extends JPanel implements ChangeListener {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -1593131642254577079L;


    /**
     * Editor of the violations.
     */
    private final ViolationsEditor editor;


    /**
     * Top percentage to take into account.
     */
    private int topPercent;


    /**
     * Indicates if duplicates should be included.
     */
    private boolean includeDuplicates;


    /**
     * The panel with the summary information.
     */
    private JPanel infoPanel;


    /**
     * Label with the total number of violations in the top percent.
     */
    private JLabel totalValue;


    /**
     * Label with the true positive rate.
     */
    private JLabel rateValue;


    /**
     * Chart with the true positive rate.
     */
    private JPanel truePositiveRateChart;


    /**
     * Mapping from violation types to labels showing their number.
     */
    private Map<ViolationType, JLabel> violationType2Value;


    /**
     * Creates a new statistics tab.
     *
     * @param editor Editor, in which the tab is created.
     */
    public StatisticsTab(ViolationsEditor editor) {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.editor = editor;
        this.topPercent = 10;
        this.includeDuplicates = true;
        createTheGUI();
    }


    /* (non-Javadoc)
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof JSpinner) {
            JSpinner spinner = (JSpinner) e.getSource();
            SpinnerNumberModel spinnerModel =
                    (SpinnerNumberModel) spinner.getModel();
            this.topPercent = spinnerModel.getNumber().intValue();
            updateTheGUI();
        } else if (e.getSource() instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            this.includeDuplicates = checkBox.isSelected();
            updateTheGUI();
        } else {
            System.err.println("Unknown event: " + e);
            throw new InternalError();
        }
    }


    /**
     * Creates the GUI of this statistics tab.
     */
    private void createTheGUI() {
        // add a panel with the details about top violations
        this.infoPanel = createInfoPanel();
        this.infoPanel.setBackground(Color.WHITE);
        this.infoPanel.setForeground(Color.WHITE);
        this.infoPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.add(this.infoPanel);

        // add the chart with the true positive rate plot
        this.truePositiveRateChart = createTruePositiveRatePlot();
        this.truePositiveRateChart.setBackground(Color.WHITE);
        this.truePositiveRateChart.setForeground(Color.WHITE);
        truePositiveRateChart.setAlignmentX(LEFT_ALIGNMENT);
        this.add(truePositiveRateChart);

        updateTheGUI();
    }


    /**
     * Updates the GUI to reflect the currently selected top percent.
     */
    public void updateTheGUI() {
        // get the violations list
        ViolationsList violations = this.editor.getViolations();

        // update the total number of violations in the top percentage
        int violationsNum = this.topPercent * violations.size() / 100;
        if (violationsNum > 0) {
            while (violationsNum < violations.size() &&
                    violations.get(violationsNum).getDefectIndicator().equals(
                            violations.get(violationsNum - 1).getDefectIndicator())) {
                violationsNum++;
            }
        }
        List<Integer> allViolationsIds = new ArrayList<Integer>();
        for (int id = 1; id <= violations.size(); id++) {
            if (includeDuplicates) {
                allViolationsIds.add(id);
            } else {
                List<Integer> duplicates = violations.getDuplicates(id);
                if (duplicates.isEmpty())
                    allViolationsIds.add(id);
                else if (!allViolationsIds.contains(duplicates.get(0)))
                    allViolationsIds.add(id);
            }
        }
        int allViolationsNum = allViolationsIds.size();
        List<Integer> violationsIds = new ArrayList<Integer>();
        for (int id : allViolationsIds) {
            if (id <= violationsNum)
                violationsIds.add(id);
        }
        violationsNum = violationsIds.size();

        int realPercentage = (100 * violationsNum) / allViolationsNum;
        if ((100 * violationsNum) % allViolationsNum != 0) {
            realPercentage++;
        }
        this.totalValue.setText(violationsNum + " / " + allViolationsNum +
                " (" + realPercentage + "%)");

        // update the true positive rate
        int truePositives = 0;
        for (int id : violationsIds) {
            if (violations.get(id - 1).getType().isTruePositive()) {
                truePositives++;
            }
        }
        int truePositiveRate = 0;
        if (violationsNum > 0) {
            truePositiveRate = 100 * truePositives / violationsNum;
        }
        this.rateValue.setText(truePositives + " (" + truePositiveRate + "%)");

        // update the numbers of different violation types
        Map<ViolationType, Integer> type2number = new HashMap<ViolationType, Integer>();
        for (ViolationType type : ViolationType.values()) {
            type2number.put(type, 0);
        }
        for (int id : violationsIds) {
            ViolationType type = violations.get(id - 1).getType();
            type2number.put(type, type2number.get(type) + 1);
        }
        for (ViolationType type : this.violationType2Value.keySet()) {
            int number = type2number.get(type);
            int rate = 0;
            if (violationsNum > 0) {
                rate = 100 * number / violationsNum;
            }
            JLabel typeValue = this.violationType2Value.get(type);
            typeValue.setText(number + " (" + rate + "%)");
        }

        // update the true positive rate chart
        this.remove(this.truePositiveRateChart);
        this.truePositiveRateChart = createTruePositiveRatePlot();
        this.truePositiveRateChart.setAlignmentX(LEFT_ALIGNMENT);
        this.add(this.truePositiveRateChart);

        // repaint the GUI
        this.infoPanel.setMaximumSize(this.infoPanel.getPreferredSize());
        this.revalidate();
        this.repaint();
    }


    /**
     * Creates the info panel with the details about top violations.
     *
     * @return Info panel with the details about top violations.
     */
    private JPanel createInfoPanel() {
        // create the panel
        JPanel info = new JPanel(new GridBagLayout());
        info.setBackground(Color.WHITE);
        info.setForeground(Color.WHITE);
        TitledBorder infoBorder = BorderFactory.createTitledBorder("Summary");
        info.setBorder(infoBorder);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = -1;

        // add the spinner with the percentage
        JLabel percentageLabel = new JLabel("Top violations [%]: ");
        SpinnerModel percentageSpinnerModel = new SpinnerNumberModel(
                this.topPercent, 0, 100, 1);
        JSpinner percentageSpinner = new JSpinner(percentageSpinnerModel);
        percentageSpinner.addChangeListener(this);
        c.gridy++;
        c.gridx = 0;
        info.add(percentageLabel, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        info.add(percentageSpinner, c);
        c.fill = GridBagConstraints.BOTH;

        // add the checkbox with duplicates info
        JCheckBox duplicatesCheckBox = new JCheckBox("Include duplicates", true);
        duplicatesCheckBox.addChangeListener(this);
        c.gridy++;
        c.gridx = 0;
        info.add(duplicatesCheckBox, c);

        // add the horizontal line
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 0);
        info.add(new JSeparator(), c);
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 0, 0);

        // add the number of violations
        JLabel totalLabel = new JLabel("No. of violations: ");
        this.totalValue = new JLabel("(n/a)");
        c.gridy++;
        c.gridx = 0;
        info.add(totalLabel, c);
        c.gridx = 1;
        info.add(this.totalValue, c);

        // add the true positive rate
        JLabel rateLabel = new JLabel("True positives: ");
        this.rateValue = new JLabel("(n/a)");
        c.gridy++;
        c.gridx = 0;
        info.add(rateLabel, c);
        c.gridx = 1;
        info.add(this.rateValue, c);

        // add the numbers of different violation types
        this.violationType2Value = new HashMap<ViolationType, JLabel>();
        for (ViolationType type : ViolationType.values()) {
            JLabel typeLabel = new JLabel("Violations of type \"" +
                    type.getStringRepresentation() + "\": ");
            JLabel typeValue = new JLabel("(n/a)");
            this.violationType2Value.put(type, typeValue);
            c.gridy++;
            c.gridx = 0;
            info.add(typeLabel, c);
            c.gridx = 1;
            info.add(typeValue, c);
        }

        // finish creating the panel and return it
        info.setMaximumSize(info.getPreferredSize());
        return info;
    }


    /**
     * Creates a plot that shows the true positive rate vs. number of violations
     * investigated.
     *
     * @return True positive rate plot.
     */
    private JPanel createTruePositiveRatePlot() {
        // prepare the data
        ViolationsList violations = this.editor.getViolations();
        List<Integer> allViolationsIds = new ArrayList<Integer>();
        for (int id = 1; id <= violations.size(); id++) {
            if (includeDuplicates) {
                allViolationsIds.add(id);
            } else {
                List<Integer> duplicates = violations.getDuplicates(id);
                if (duplicates.isEmpty())
                    allViolationsIds.add(id);
                else if (!allViolationsIds.contains(duplicates.get(0)))
                    allViolationsIds.add(id);
            }
        }

        int[] topViolations = new int[101];
        int[] truePositives = new int[101];
        int lastViolationsNum = 0;
        for (int percentage = 0; percentage <= 100; percentage++) {
            int violationsNum = percentage * violations.size() / 100;
            if (violationsNum > lastViolationsNum) {
                while (violationsNum < violations.size() &&
                        violations.get(violationsNum).getDefectIndicator().equals(
                                violations.get(violationsNum - 1).getDefectIndicator())) {
                    violationsNum++;
                }
            } else {
                violationsNum = lastViolationsNum;
            }
            List<Integer> violationsIds = new ArrayList<Integer>();
            for (int id : allViolationsIds) {
                if (id <= violationsNum)
                    violationsIds.add(id);
            }
            violationsNum = violationsIds.size();
            topViolations[percentage] = violationsNum;
            truePositives[percentage] = 0;
            for (int id : violationsIds) {
                if (violations.get(id - 1).getType().isTruePositive()) {
                    truePositives[percentage]++;
                }
            }
            lastViolationsNum = violationsNum;
        }

        // create the bar chart
        XYSeries series = new XYSeries("True positive rate");
        double maximumRate = 0.0;
        for (int percentage = 0; percentage <= 100; percentage++) {
            double rate = 0.0;
            if (topViolations[percentage] > 0) {
                rate = 100.0 * (double) truePositives[percentage] /
                        (double) topViolations[percentage];
            }
            if (rate > maximumRate) {
                maximumRate = rate;
            }
            series.add(percentage, rate);
        }
        XYSeriesCollection seriesCollection = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYBarChart(null,
                "Top violations [%]", false, "True positives", seriesCollection,
                PlotOrientation.VERTICAL, false, true, false);
        XYPlot plot = chart.getXYPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setRange(0.0, 100.0);
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);
        chart.setBackgroundPaint(Color.WHITE);
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setShadowVisible(false);
        renderer.setMargin(0.6);

        // create the panel
        BufferedImage image = chart.createBufferedImage(1200, 400);
        ImageIcon icon = new ImageIcon(image);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(icon);
        JPanel panel = new JPanel();
        panel.setForeground(Color.WHITE);
        panel.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder("True positive rate vs. top violations [%]");
        panel.setBorder(border);
        panel.add(imageLabel);
        panel.setMaximumSize(panel.getPreferredSize());
        return panel;
    }
}
