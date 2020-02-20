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

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Used for generating actor usage models of scratch programs.
 */
public class AUMExtractor {
    /**
     * Logger to be used by this class.
     */
    private final static Logger logger = Logger.getLogger("org.softevo.oumextractor");

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

    static {
        try {
            Handler handler = new FileHandler("OUMExtractor.log");
            handler.setFormatter(new SimpleFormatter());
            AUMExtractor.logger.addHandler(handler);
            AUMExtractor.logger.setLevel(Level.OFF);
        } catch (IOException e) {
            System.err.println("[ERROR] Couldn't open log file");
        }
    }

    /**
     * Creates new instance of this class.
     */
    public AUMExtractor() {}

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
     *                            programs to analyse.
     * @param pathToOutputFolder Path to the folder in which the actor usage
     *                           models are created.
     */
    public void createActorUsageModels(String pathToAnalysisFolder, String pathToOutputFolder) {
        //TODO
    }
}
