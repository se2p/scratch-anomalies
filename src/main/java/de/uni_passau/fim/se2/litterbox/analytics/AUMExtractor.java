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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_passau.fim.se2.litterbox.ast.ParsingException;
import de.uni_passau.fim.se2.litterbox.ast.model.Program;
import de.uni_passau.fim.se2.litterbox.ast.parser.ProgramParser;
import de.uni_passau.fim.se2.litterbox.ast.visitor.AUMVisitor;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Used for generating actor usage models of scratch programs.
 */
public class AUMExtractor {
    /**
     * Logger to be used by this class.
     */
    private final static Logger logger = Logger.getLogger(AUMExtractor.class.getName());

    /**
     * Number of projects present.
     */
    private static int projectsPresent = 0;

    /**
     * Number of scripts present in the analysed json files.
     */
    private static int scriptsPresent = 0;

    /**
     * Number of analysed scripts.
     */
    private static int scriptsAnalysed = 0;

   /* static {
        try {
            Handler handler = new FileHandler("AUMExtractor.log");
            handler.setFormatter(new SimpleFormatter());
            AUMExtractor.logger.addHandler(handler);
            AUMExtractor.logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("[ERROR] Couldn't open log file");
        }
    }*/

    /**
     * Creates new instance of this class.
     */
    public AUMExtractor() {
    }

    /**
     * This method should be called whenever analysis of a new script is
     * starting.  This ensures that the number of scripts present is correct.
     */
    public static void newProjectPresent() {
        projectsPresent++;
    }

    /**
     * This method should be called whenever analysis of a new script is
     * starting.  This ensures that the number of scripts present is correct.
     */
    public static void newScriptPresent() {
        scriptsPresent++;
    }

    /**
     * This method should be called whenever analysis of a new script is
     * completed without errors. This ensures that the number of scripts
     * analysed is correct.
     */
    public static void newScriptAnalysed() {
        scriptsAnalysed++;
    }

    /**
     * Creates actor usage models for the given scratch programs.
     *
     * @param pathToAnalysisFolder Path to the folder containing the scratch
     *                             programs to analyse.
     * @param pathToOutputFolder   Path to the folder in which the actor usage
     *                             models are created.
     */
    public void createActorUsageModels(String pathToAnalysisFolder, String pathToOutputFolder) {
        File analysisFolder = new File(pathToAnalysisFolder);
        if (!analysisFolder.exists()) {
            logger.severe("Analysis folder does not exist: " + analysisFolder);
        } else {
            File outputFolder = new File(pathToOutputFolder);
            if (!outputFolder.exists()) {
                logger.info("Creating output folder: " + pathToOutputFolder);
                if (!outputFolder.mkdirs()) {
                    logger.severe("Failed to create output folder: " + pathToOutputFolder);
                }
            }
            Set<String> programs = new HashSet<>();
            for (File fileEntry : requireNonNull(analysisFolder.listFiles())) {
                if ((FilenameUtils.getExtension(fileEntry.getPath())).toLowerCase().equals("json")) {
                    programs.add(fileEntry.getName());
                }
            }
            AUMVisitor visitor = new AUMVisitor(pathToOutputFolder, programs);
            for (File fileEntry : requireNonNull(analysisFolder.listFiles())) {
                ObjectMapper mapper = new ObjectMapper();
                Program program;
                if ((FilenameUtils.getExtension(fileEntry.getPath())).toLowerCase().equals("json")) {
                    try {
                        program = ProgramParser.parseProgram(fileEntry.getName(), mapper.readTree(fileEntry));
                    } catch (ParsingException | IOException e) {
                        logger.severe("Unable to parse project: " + fileEntry.getAbsolutePath());
                        continue;
                    }
                    program.accept(visitor);
                    visitor.shutdownAnalysis();
                }
            }
        }


    }
}
