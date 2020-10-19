package org.softevo.jadet;


import org.softevo.cmdline.CommandLine;
import org.softevo.cmdline.InputFormat;
import org.softevo.cmdline.SwitchMultiplicity;
import org.softevo.jadet.sca.OutputVerbosity;
import org.softevo.jadet.sca.ViolationsList;
import org.softevo.jadet.violations.editor.ViolationsEditor;
import org.softevo.jutil.JavaUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 * This is the main class of the part of the project, that creates models of object
 * usage based on static analysis.
 *
 * @author Andrzej Wasylkowski
 */
public final class JADET {

    /**
     * Logger to be used by this class.
     */
    private final static Logger logger =
            Logger.getLogger("org.softevo.jadet");


    static {
        try {
            Handler handler = new FileHandler("JADET.log");
            handler.setFormatter(new SimpleFormatter());
            JADET.logger.addHandler(handler);
            JADET.logger.setLevel(Level.OFF);
        } catch (IOException e) {
            System.err.println("[ERROR] Couldn't open log file");
        }
    }


    /**
     * Creates new instance of this class.  It is to be used only internally, in
     * order to hold values extracted from the command-line.
     */
    private JADET() {
    }

    /**
     * Creates models of object usage based on static analysis.
     *
     * @param args List of command-line arguments
     */
    public static void main(String[] args) {
        // create the analyzer object and run analysis according to command line
        JADET analyzer = new JADET();
        analyzer.runCommandLine(JavaUtil.removeEmptyArguments(args));
    }

    /**
     * Parses given command-line arguments and sets flags in this instance
     * accordingly.  Afterwards runs analysis as specified by the command-line.
     *
     * @param args List of command-line arguments
     */
    private void runCommandLine(String[] args) {
        // generate command line format
        CommandLine cmdLine = new CommandLine(this.getClass().getSimpleName());

        InputFormat outputPatterns =
                new InputFormat("to output patterns in sca abstraction");
        outputPatterns.addSwitch("output-patterns",
                "makes JADET output ranked patterns in the sequential " +
                        "constraints abstraction",
                SwitchMultiplicity.ONE);
        outputPatterns.addSwitch("xml", "makes JADET use XML format",
                SwitchMultiplicity.ZERO_OR_ONE);
        outputPatterns.addSwitch("short", "makes JADET output short event " +
                "representations", SwitchMultiplicity.ZERO_OR_ONE);
        outputPatterns.addSwitch("very-short", "makes JADET output very " +
                "short event representations", SwitchMultiplicity.ZERO_OR_ONE);
        outputPatterns.addSwitch("models-dir", "specifies the directory " +
                "with models to use", SwitchMultiplicity.ZERO_OR_ONE, true);
        outputPatterns.addSwitch("input-name", "specifies the core name " +
                "of the input files", SwitchMultiplicity.ZERO_OR_ONE, true);
        outputPatterns.setDataCount(3);
        outputPatterns.setDataName(1, "min_support");
        outputPatterns.setDataName(2, "min_size");
        outputPatterns.setDataName(3, "out_file");
        cmdLine.addInputFormat("output-patterns", outputPatterns);

        InputFormat outputViolationsXML =
                new InputFormat("to output violations in sca abstraction");
        outputViolationsXML.addSwitch("output-violations-xml",
                "makes JADET output ranked violations in the sequential " +
                        "constraints abstraction in the XML format",
                SwitchMultiplicity.ONE);
        outputViolationsXML.addSwitch("models-dir", "specifies the directory " +
                "with models to use", SwitchMultiplicity.ZERO_OR_ONE, true);
        outputViolationsXML.addSwitch("input-name", "specifies the core name " +
                "of the input files", SwitchMultiplicity.ZERO_OR_ONE, true);
        outputViolationsXML.setDataCount(5);
        outputViolationsXML.setDataName(1, "min_support");
        outputViolationsXML.setDataName(2, "min_size");
        outputViolationsXML.setDataName(3, "max_dev_level");
        outputViolationsXML.setDataName(4, "min_confidence");
        outputViolationsXML.setDataName(5, "out_file");
        cmdLine.addInputFormat("output-violations-xml", outputViolationsXML);

        InputFormat editViolationsXML =
                new InputFormat("to view and edit sca abstraction violations");
        editViolationsXML.addSwitch("edit-violations-xml",
                "makes JADET run a viewer/editor of violations contained in " +
                        "the provided XML file", SwitchMultiplicity.ONE);
        editViolationsXML.setDataCount(1);
        editViolationsXML.setDataName(1, "violations_file");
        cmdLine.addInputFormat("edit-violations-xml", editViolationsXML);

        InputFormat copyViolationsXML =
                new InputFormat("to copy violations details between files");
        copyViolationsXML.addSwitch("copy-violations-xml",
                "makes JADET compare two violations list and copy details " +
                        "for identical violations from the second to the first list",
                SwitchMultiplicity.ONE);
        copyViolationsXML.setDataCount(2);
        copyViolationsXML.setDataName(1, "target_violations_file");
        copyViolationsXML.setDataName(2, "source_violations_file");
        cmdLine.addInputFormat("copy-violations-xml", copyViolationsXML);

        InputFormat visualizePattern =
                new InputFormat("to output a visualization of a given pattern");
        visualizePattern.addSwitch("visualize-pattern",
                "makes JADET output a visualization (in the dot-format) of " +
                        "a pattern from the given file (in the textual format)",
                SwitchMultiplicity.ONE);
        visualizePattern.addSwitch("thesis", "trims the pattern to thesis-format");
        visualizePattern.setDataCount(2);
        visualizePattern.setDataName(1, "dot_file");
        visualizePattern.setDataName(2, "pattern_file");
        cmdLine.addInputFormat("visualize-pattern", visualizePattern);

        InputFormat visualizeAll =
                new InputFormat("to output the visualization of all patterns of the JADET pattern file");
        visualizeAll.addSwitch("visualize-all", "makes JADET output a visualization (dot-format) of "
                + "all the patterns in the given file (JADET output format)",
                SwitchMultiplicity.ONE);
        visualizeAll.setDataCount(2);
        visualizeAll.setDataName(1, "dot_folder");
        visualizeAll.setDataName(2, "pattern_file");
        cmdLine.addInputFormat("visualize-all", visualizeAll);

        cmdLine.addHelpFormat();

        // parse command line
        if (cmdLine.parseCommandLine(args)) {
            if (cmdLine.getFormatName().equals("output-patterns")) {
                String minSupportString = cmdLine.getDataValue(1);
                Integer minSupport = Integer.valueOf(minSupportString);
                String minSizeString = cmdLine.getDataValue(2);
                Integer minSize = Integer.valueOf(minSizeString);
                String outFileName = cmdLine.getDataValue(3);
                boolean xmlSwitch = cmdLine.getSwitchCount("xml") > 0;
                boolean shortSwitch = cmdLine.getSwitchCount("short") > 0;
                boolean veryShortSwitch =
                        cmdLine.getSwitchCount("very-short") > 0;
                String modelsDirName = null;
                if (cmdLine.getSwitchCount("models-dir") > 0)
                    modelsDirName = cmdLine.getSwitchValues("models-dir")[0];
                String inputName = null;
                if (cmdLine.getSwitchCount("input-name") > 0)
                    inputName = cmdLine.getSwitchValues("input-name")[0];
                outputPatterns(modelsDirName, inputName, minSupport, minSize,
                        outFileName, xmlSwitch, shortSwitch, veryShortSwitch);
            } else if (cmdLine.getFormatName().equals("output-violations-xml")) {
                String minSupportString = cmdLine.getDataValue(1);
                Integer minSupport = Integer.valueOf(minSupportString);
                String minSizeString = cmdLine.getDataValue(2);
                Integer minSize = Integer.valueOf(minSizeString);
                String maxDevLevelString = cmdLine.getDataValue(3);
                Integer maxDevLevel = Integer.valueOf(maxDevLevelString);
                String minConfidenceString = cmdLine.getDataValue(4);
                Double minConfidence = Double.valueOf(minConfidenceString);
                String outFileName = cmdLine.getDataValue(5);
                String modelsDirName = null;
                if (cmdLine.getSwitchCount("models-dir") > 0)
                    modelsDirName = cmdLine.getSwitchValues("models-dir")[0];
                String inputName = null;
                if (cmdLine.getSwitchCount("input-name") > 0)
                    inputName = cmdLine.getSwitchValues("input-name")[0];
                outputViolationsXML(modelsDirName, inputName, minSupport,
                        minSize, maxDevLevel, minConfidence, outFileName);
            } else if (cmdLine.getFormatName().equals("edit-violations-xml")) {
                String violationsFileName = cmdLine.getDataValue(1);
                editViolationsXML(violationsFileName);
            } else if (cmdLine.getFormatName().equals("copy-violations-xml")) {
                String targetViolationsFileName = cmdLine.getDataValue(1);
                String sourceViolationsFileName = cmdLine.getDataValue(2);
                copyViolationsXML(targetViolationsFileName,
                        sourceViolationsFileName);
            } else if (cmdLine.getFormatName().equals("visualize-pattern")) {
                boolean thesis = false;
                if (cmdLine.getSwitchCount("thesis") > 0)
                    thesis = true;
                String dotFileName = cmdLine.getDataValue(1);
                String patternFileName = cmdLine.getDataValue(2);
                visualizePattern(dotFileName, patternFileName, thesis);
            } else if (cmdLine.getFormatName().equals("visualize-all")) {
                String dotFolderName = cmdLine.getDataValue(1);
                String patternFileName = cmdLine.getDataValue(2);
                visualizeAll(dotFolderName, patternFileName);
            } else {
                cmdLine.processOtherFormats();
            }
        } else {
            System.err.println(cmdLine.getErrorString());
            cmdLine.printShortHelp();
        }
    }

    /**
     * Mines, ranks and outputs patterns for sequential constraints abstraction.
     *
     * @param modelsDirName   Directory with the models to analyze (optional).
     * @param inputName       Core name of files with sca (optional).
     * @param minSupport      Minimum support to use when mining patterns.
     * @param minSize         Minimum size to use when mining patterns.
     * @param outFileName     Filename to output patterns to.
     * @param xmlSwitch       Indicates, if the "-xml" switch was present.
     * @param shortSwitch     Indicates, if the "-short" switch was present.
     * @param veryShortSwitch Indicates, if the "-very-short" switch was
     *                        present.
     */
    private void outputPatterns(String modelsDirName, String inputName,
                                int minSupport, int minSize, String outFileName, boolean xmlSwitch,
                                boolean shortSwitch, boolean veryShortSwitch) {
        if (modelsDirName == null && inputName == null) {
            System.out.println("At least one of -models-dir and -input-name " +
                    "switches must be used");
            return;
        }
        if (modelsDirName != null && inputName != null) {
            System.out.println("Only one of -models-dir and -input-name " +
                    "switches can be used");
            return;
        }
        if (minSupport < 1) {
            System.out.println("Minimum support must be >= 1 (given: " +
                    minSupport + ")");
            return;
        }
        if (minSize < 1) {
            System.out.println("Minimum size must be >= 1 (given: " +
                    minSize + ")");
            return;
        }
        if (xmlSwitch && (shortSwitch || veryShortSwitch)) {
            System.out.println("When using the -xml switch, -short and " +
                    "-very-short switches are ignored.");
        } else if (shortSwitch && veryShortSwitch) {
            System.out.println("Only one of -short and -very-short switches " +
                    "can be used");
            return;
        }
        Analyzer analyzer = new Analyzer();
        analyzer.minePatterns(modelsDirName, inputName, minSupport, minSize);
        OutputVerbosity outputVerbosity = OutputVerbosity.FULL;
        if (shortSwitch) {
            outputVerbosity = OutputVerbosity.SHORT;
        }
        if (veryShortSwitch) {
            outputVerbosity = OutputVerbosity.VERY_SHORT;
        }
        if (xmlSwitch)
            analyzer.outputPatternsXML(new File(outFileName));
        else
            analyzer.outputPatterns(new File(outFileName), outputVerbosity);
    }

    /**
     * Mines, ranks and outputs violations for sequential constraints abstraction.
     *
     * @param modelsDirName Directory with the models to analyze (optional).
     * @param inputName     Core name of files with sca (optional).
     * @param minSupport    Minimum support to use.
     * @param minSize       Minimum size to use.
     * @param maxDevLevel   Maximum deviation level to use.
     * @param minConfidence Minimum confidence to use.
     * @param outFileName   Filename to output patterns to.
     */
    private void outputViolationsXML(String modelsDirName, String inputName,
                                     int minSupport, int minSize, int maxDevLevel, double minConfidence,
                                     String outFileName) {
        if (modelsDirName == null && inputName == null) {
            System.out.println("At least one of -models-dir and -input-name " +
                    "switches must be used");
            return;
        }
        if (modelsDirName != null && inputName != null) {
            System.out.println("Only one of -models-dir and -input-name " +
                    "switches can be used");
            return;
        }
        if (minSupport < 1) {
            System.out.println("Minimum support must be >= 1 (given: " +
                    minSupport + ")");
            return;
        }
        if (minSize < 1) {
            System.out.println("Minimum size must be >= 1 (given: " +
                    minSize + ")");
            return;
        }
        if (maxDevLevel < 1) {
            System.out.println("Maximum deviation level must be >= 1 (given: " +
                    maxDevLevel + ")");
            return;
        }
        if (minConfidence <= 0.0 || minConfidence >= 1.0) {
            System.out.println("Minimum confidence must be > 0.0 and < 1.0 " +
                    "(given: " + minConfidence + ")");
            return;
        }
        Analyzer analyzer = new Analyzer();
        analyzer.mineViolations(modelsDirName, inputName, minSupport, minSize,
                maxDevLevel, minConfidence);
        analyzer.outputViolationsXML(new File(outFileName));
    }

    /**
     * Opens a GUI viewer/editor of violations from the given XML file.
     *
     * @param violationsFileName Name of the file with XML violations.
     */
    private void editViolationsXML(String violationsFileName) {
        ViolationsEditor editor = new ViolationsEditor(
                new File(violationsFileName));
        SwingUtilities.invokeLater(editor);
    }

    /**
     * Copies information about identical violations from the source list to
     * the target list.
     *
     * @param targetViolationsFileName Name of the file with XML violations to update.
     * @param sourceViolationsFileName Name of the file with reference XML violations.
     */
    private void copyViolationsXML(String targetViolationsFileName,
                                   String sourceViolationsFileName) {
        // read both violations lists
        File targetViolationsFile = new File(targetViolationsFileName);
        ViolationsList targetViolations =
                ViolationsList.readFromXML(targetViolationsFile);
        File sourceViolationsFile = new File(sourceViolationsFileName);
        ViolationsList sourceViolations =
                ViolationsList.readFromXML(sourceViolationsFile);

        // update the target violations list
        targetViolations.copyDetailsFrom(sourceViolations,
                sourceViolationsFileName);

        // save the target violations list
        targetViolations.writeXML(targetViolationsFile);
    }

    /**
     * Visualizes the pattern from the given input file and puts its dot-format
     * visualization in the given output file.
     *
     * @param dotFileName     Name of the file to put the dot-format visualization to.
     * @param patternFileName Name of the file to read the pattern from.
     * @param thesis          Indicates if thesis-trimming should be done.
     */
    private void visualizePattern(String dotFileName, String patternFileName,
                                  boolean thesis) {
        File dotFile = new File(dotFileName);
        File patternFile = new File(patternFileName);
        PatternVisualizer visualizer = new PatternVisualizer(thesis);
        visualizer.visualize(dotFile, patternFile);
    }

    private void visualizeAll(String dotFolderName, String patternFileName) {
        PatternVisualizer visualizer = new PatternVisualizer(false);
        try {
            visualizer.visualizeAll(new File(dotFolderName), new File(patternFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
