/*
 * Copyright (C) 2019 LitterBox contributors
 *
 * This file is part of LitterBox.
 *
 * LitterBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * LitterBox is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LitterBox. If not, see <http://www.gnu.org/licenses/>.
 */
package de.uni_passau.fim.se2.litterbox.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_passau.fim.se2.litterbox.ast.ParsingException;
import de.uni_passau.fim.se2.litterbox.ast.model.Program;
import de.uni_passau.fim.se2.litterbox.ast.parser.ProgramParser;
import de.uni_passau.fim.se2.litterbox.ast.visitor.ScriptModelVisitor;
import de.uni_passau.fim.se2.litterbox.utils.JsonParser;
import de.uni_passau.fim.se2.litterbox.utils.ZipReader;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Used for generating actor usage models of scratch programs.
 *
 * @author Nina Körber
 */
public class ScriptModelGenerator {
    /**
     * Logger to be used by this class.
     */
    private final static Logger logger = Logger.getLogger(ScriptModelGenerator.class.getName());

    /**
     * Start time of the analysis.
     */
    private final LocalDateTime start = LocalDateTime.now();

    /**
     * Folder in which the programs script models of which are to be created are.
     */
    private final File analysisFolder;

    /**
     * Destination for the script models and the modelsdata, typesnames, index, summary and
     * exceptions files.
     */
    private final File outputFolder;

    /**
     * Path to a folder in which the dotfile representation of the script models has to be
     * stored. Null if no dotfile output is requested.
     */
    private final String dotOutputPath;

    /**
     * The stream for printing the summary of the analysis.
     */
    private PrintStream summaryStream;

    /**
     * The stream for printing the stack traces of the analysis.
     */
    private PrintStream exceptionsStream;

    /**
     * The names of all programs in the analysis folder.
     * This information is necessary for JADET.
     */
    private Set<String> programs;

    /**
     * The visitor used for creating the script models.
     */
    private final ScriptModelVisitor visitor;

    /**
     * Number of projects present.
     */
    private int projectsPresent = 0;

    /**
     * Number of scripts present in the analysed json files.
     */
    private int scriptsPresent = 0;

    /**
     * Number of analysed scripts.
     */
    private int scriptsAnalysed = 0;

    /**
     * Number of procedure definitions present in the analysed json files.
     */
    private int procDefsPresent = 0;

    /**
     * Number of analysed procedure definitions.
     */
    private int procDefsAnalysed = 0;

    /**
     * Number of programs which were analysed successfully.
     */
    private int successfullyAnalysed = 0;

    /**
     * Number of programs analysis of which did not succeed because the
     * parsing failed.
     */
    private int skippedDueToParsing = 0;

    /**
     * Number of programs analysis of which did not succeed because of an exception
     * in the {@code visitor}.
     */
    private int skippedDueToScriptModelGenerator = 0;

    /**
     * Number of programs which were processed already, regardless of the result.
     */
    private int programsProcessed = 0;

    /**
     * Number of exceptions which occurred during parsing and creation of the
     * script models.
     */
    private int exceptionsOccurred = 0;

    /**
     * Makes sure the analysis folder exists and sets up everything else:
     * Creates and empties the output folder(s), sets the set of program names
     * for the visitor and sets up the streams for summary and exception output.
     *
     * @param analysisFolderPath    Path to the folder containing the scratch
     *                              programs to analyse.
     * @param dotOutputPath         Path to the folder in which the dot files of the
     *                              actor usage models are created.
     * @param scriptModelOutputPath Path to the folder in which the actor usage
     *                              models are created.
     * @throws FileNotFoundException If creation of the print streams fails.
     */
    public ScriptModelGenerator(String analysisFolderPath, String dotOutputPath, String scriptModelOutputPath) throws FileNotFoundException {
        this.dotOutputPath = dotOutputPath;

        analysisFolder = new File(analysisFolderPath);
        outputFolder = new File(scriptModelOutputPath);
        if (!analysisFolder.exists()) {
            String msg = "Analysis folder does not exist: " + analysisFolder;
            logger.severe(msg);
            throw new RuntimeException(msg);
        } else {
            prepareOutFolders();
            initStreams(scriptModelOutputPath);
            setPrograms();
            visitor = new ScriptModelVisitor(this);
        }
    }

    /**
     * Creates all non-existing required output folders and deletes any files
     * which are present in the output folders.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void prepareOutFolders() {
        if (!outputFolder.exists()) {
            String outputPath = outputFolder.getAbsolutePath();
            logger.info("Creating output folder: " + outputPath);
            if (!outputFolder.mkdirs()) {
                logger.severe("Failed to create output folder: " + outputPath);
            }
        }
        for (File file : outputFolder.listFiles()) {
            file.delete();
        }
        if (dotOutputPath != null) {
            File dotOutputFolder = new File(dotOutputPath);
            if (!dotOutputFolder.exists()) {
                logger.info("Creating dot output folder: " + dotOutputPath);
                if (!dotOutputFolder.mkdirs()) {
                    logger.severe("Failed to create dot output folder: " + dotOutputPath);
                }
            }
            for (File file : dotOutputFolder.listFiles()) {
                file.delete();
            }
        }
    }

    /**
     * Initialises the print streams for the summary and exceptions file.
     *
     * @param outputFolderPath The path to the output folder.
     * @throws FileNotFoundException If creation of the output streams fails.
     */
    private void initStreams(String outputFolderPath) throws FileNotFoundException {
        summaryStream = new PrintStream(new FileOutputStream(outputFolderPath + File.separator + "summary.txt"));
        exceptionsStream = new PrintStream(new FileOutputStream(outputFolderPath + File.separator + "exceptions.txt"));
        exceptionsStream.println("Exceptions of another successful LitterBox ScriptModelGenerator run:");
        exceptionsStream.println("Start of analysis: " + start.format(DateTimeFormatter.ISO_DATE)
                + " " + start.format(DateTimeFormatter.ISO_TIME));
        exceptionsStream.println();
    }

    /**
     * Adds all json files to the {@code programs}.
     */
    private void setPrograms() {
        programs = new HashSet<>();
        for (File fileEntry : requireNonNull(analysisFolder.listFiles())) {
            if (isJson(fileEntry) || isSb3(fileEntry)) {
                programs.add(fileEntry.getName());
            }
        }
    }

    /**
     * Checks whether this file is a JSON file.
     *
     * @param fileEntry The file to be checked.
     * @return {@code true} iff the file is a JSON file.
     */
    private boolean isJson(File fileEntry) {
        return (FilenameUtils.getExtension(fileEntry.getPath())).toLowerCase().equals("json");
    }

    /**
     * Checks whether this file is a sb3 file.
     *
     * @param fileEntry The file to be checked.
     * @return {@code true} iff the file is a sb3 file.
     */
    private boolean isSb3(File fileEntry) {
        return (FilenameUtils.getExtension(fileEntry.getPath())).toLowerCase().equals("sb3");
    }

    /**
     * Returns the path to the folder in which the dot files should be stored.
     * Is null if dot output is not requested.
     *
     * @return The path to the folder in which the dot files should be stored.
     */
    public String getDotOutputPath() {
        return dotOutputPath;
    }

    /**
     * Returns the path to the destination for the script models and the modelsdata,
     * typesnames, index, summary and exceptions files.
     *
     * @return The path of the output folder of {@code this}.
     */
    public String getOutputFolderPath() {
        return outputFolder.getAbsolutePath();
    }

    /**
     * Returns the names of all programs in the analysis folder.
     *
     * @return The names of all programs in the analysis folder.
     */
    public Set<String> getPrograms() {
        return programs;
    }

    /**
     * This method should be called whenever the analysis of a project is starting.
     * This ensures that the number of projects present is correct.
     */
    public void newProjectPresent() {
        projectsPresent++;
    }

    /**
     * This method should be called whenever the analysis of a new script is
     * starting. This ensures that the number of scripts present is correct.
     */
    public void newScriptPresent() {
        scriptsPresent++;
    }

    /**
     * This method should be called whenever analysis of a new script
     * completed without errors. This ensures that the number of scripts
     * analysed is correct.
     */
    public void newScriptAnalysed() {
        scriptsAnalysed++;
    }

    /**
     * This method should be called whenever the analysis of a new procedure
     * definition is starting. This ensures that the number of procedure
     * definitions present is correct.
     */
    public void newProcDefPresent() {
        procDefsPresent++;
    }

    /**
     * This method should be called whenever the analysis of a new procedure
     * definition is completed without errors. This ensures that the number of
     * procedure definitions analysed is correct.
     */
    public void newProcDefAnalysed() {
        procDefsAnalysed++;
    }

    /**
     * Creates actor usage models for the scratch programs in the analysis folder.
     */
    public void runAnalysis() throws IOException, ParsingException {
        for (File fileEntry : requireNonNull(analysisFolder.listFiles())) {
            if (isJson(fileEntry)) {
                programsProcessed++;
                int percentProcessed = (100 * programsProcessed / programs.size());
                System.out.println("Analysing " + programsProcessed + "/" + programs.size() + " ("
                        + percentProcessed + "% done): " + fileEntry.getName());
                Program program = parseProgramFromFile(fileEntry);
                if (program != null) {
                    createActorUsageModels(program, fileEntry);
                }
            } else {
                if (isSb3(fileEntry)) {
                    programsProcessed++;
                    int percentProcessed = (100 * programsProcessed / programs.size());
                    System.out.println("Analysing " + programsProcessed + "/" + programs.size() + " ("
                            + percentProcessed + "% done): " + fileEntry.getName());
                    String jsonString = ZipReader.getJsonString(fileEntry.getPath());
                    JsonNode node = JsonParser.getTargetsNodeFromJSONString(jsonString);
                    if (node == null) {
                        continue;
                    }
                    Program program = ProgramParser.parseProgram(fileEntry.getName(), node);
                    if (program != null) {
                        createActorUsageModels(program, fileEntry);
                    }
                }
            }
        }
        endAnalysis();
    }

    /**
     * Parses the program in the file.
     *
     * @param fileEntry The JSON file of a Scratch program.
     * @return The
     */
    private Program parseProgramFromFile(File fileEntry) {
        Program program;
        try {
            ObjectMapper mapper = new ObjectMapper();
            program = ProgramParser.parseProgram(fileEntry.getName(), mapper.readTree(fileEntry));
        } catch (Exception e) {
            skippedDueToParsing++;
            exceptionsOccurred++;
            printException(fileEntry, e);
            logger.severe("Unable to parse project: " + fileEntry.getAbsolutePath());
            return null;
        }
        return program;
    }

    /**
     * Creates script models for every script and procedure definition of the program.
     *
     * @param program   The program which is analysed.
     * @param fileEntry The file of the program.
     */
    private void createActorUsageModels(Program program, File fileEntry) {
        try {
            program.accept(visitor);
            successfullyAnalysed++;
        } catch (Exception e) {
            skippedDueToScriptModelGenerator++;
            exceptionsOccurred++;
            printException(fileEntry, e);
            logger.severe("Creating script model for the project failed.");
            visitor.rollbackAnalysis();
        }
    }

    /**
     * Prints the name of the program in which an exception occurred and the
     * stacktrace of the exception.
     *
     * @param program The program analysis of which resulted in an exception.
     * @param e       The exception which was thrown during analysis of the program.
     */
    private void printException(File program, Exception e) {
        exceptionsStream.println(program.getName());
        e.printStackTrace(exceptionsStream);
        exceptionsStream.println();
    }

    /**
     * Serialises all relevant information and prints the summary to both the
     * summary stream and the console.
     */
    private void endAnalysis() {
        visitor.shutdownAnalysis();
        System.out.println();
        printSummary(System.out);
        printSummary(summaryStream);
        summaryStream.close();
        exceptionsStream.close();
    }

    /**
     * Prints the duration, paths and stats of this analysis to the stream.
     *
     * @param printStream The stream to be used for printing the summary.
     */
    private void printSummary(PrintStream printStream) {
        printStream.println("Summary of another successful LitterBox ScriptModelGenerator with control blocks run:");
        printStream.println();
        printDuration(printStream);
        printStream.println();
        printPaths(printStream);
        printStream.println();
        printStats(printStream);
    }

    /**
     * Prints the paths used for the analysis.
     *
     * @param printStream The stream to be used for printing the paths.
     */
    private void printPaths(PrintStream printStream) {
        printStream.println("Analysis path: " + analysisFolder.getAbsolutePath());
        if (dotOutputPath != null) {
            printStream.println("Dot output path: " + dotOutputPath);
        } else {
            printStream.println("No dot output path specified.");
        }
        printStream.println("Output path: " + outputFolder.getAbsolutePath());
    }

    /**
     * Prints statistics about the analysis of all programs in the analysis folder.
     *
     * @param printStream The stream to be used for printing the stats.
     */
    private void printStats(PrintStream printStream) {
        printStream.println("Skipped due to parsing: " + skippedDueToParsing + " projects.");
        printStream.println("Skipped during creation of script models: " + skippedDueToScriptModelGenerator + " projects.");
        printStream.println("Total number of exceptions which occurred: " + exceptionsOccurred);
        printStream.println("Projects for which analysis started: " + projectsPresent);
        int totalProjects = skippedDueToScriptModelGenerator + skippedDueToParsing + successfullyAnalysed;
        printStream.println("Projects analysed/present: " + successfullyAnalysed + "/" + totalProjects);
        printStream.println("Scripts analysed/present: " + scriptsAnalysed + "/" + scriptsPresent);
        printStream.println("Procedure definitions analysed/present: " + procDefsAnalysed + "/" + procDefsPresent);
        printStream.println("Models created: " + visitor.getModelsExtracted());
    }

    /**
     * Prints start- and end time of the analysis and the duration of the analysis.
     *
     * @param printStream The stream to be used for printing the duration.
     */
    private void printDuration(PrintStream printStream) {
        LocalDateTime now = LocalDateTime.now();
        printStream.println("Start of analysis: " + start.format(DateTimeFormatter.ISO_DATE)
                + " " + start.format(DateTimeFormatter.ISO_TIME));
        printStream.println("End of analysis: " + now.format(DateTimeFormatter.ISO_DATE)
                + " " + now.format(DateTimeFormatter.ISO_TIME));
        Duration duration = Duration.between(start, now);
        printStream.println("Duration: " + duration.toHours() + ":" + duration.toMinutes()
                + ":" + duration.toSeconds() + "." + duration.toNanos());
    }

    /**
     * Convenience method to create dot output for the specified programs
     * without using the command line.
     *
     * @param args Command line arguments.
     * @throws FileNotFoundException if creating the ScriptModelGenerator fails.
     */
    public static void main(String[] args) throws IOException, ParsingException {
        ScriptModelGenerator extractor = new ScriptModelGenerator("src/test/fixtures/bugpattern", "out/dot", "out/ser");
        extractor.runAnalysis();
    }
}
