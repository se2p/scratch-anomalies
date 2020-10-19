package org.softevo.oumextractor.modelcreator1;

import org.softevo.oumextractor.modelcreator1.model.Model;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for analyzing models.
 *
 * @author Andrzej Wasylkowski
 */
public class ModelAnalyzer {

    /**
     * Directory with models to analyze.
     */
    private final File modelsDir;

    /**
     * Mapping from ids to data.
     */
    private final Map<Integer, ModelData> id2data;

    /**
     * Total number of models.
     */
    private final int modelsNum;

    /**
     * Indicates, if progress should be outputted while analyzing models.
     */
    private final boolean outputProgress;

    /**
     * Creates a new model analyzer that will operate on a given directory
     * and output progress while working.
     *
     * @param modelsDir Directory containing the models to analyze.
     */
    public ModelAnalyzer(File modelsDir) {
        this.modelsDir = modelsDir;
        this.id2data = readId2ModelData();
        this.modelsNum = id2data.size();
        this.outputProgress = true;
    }

    /**
     * Creates a new model analyzer that will operate on a given directory.
     *
     * @param modelsDir      Directory containing the models to analyze.
     * @param outputProgress Indicates, if progress should be outputted
     *                       while analyzing models.
     */
    public ModelAnalyzer(File modelsDir, boolean outputProgress) {
        this.modelsDir = modelsDir;
        this.id2data = readId2ModelData();
        this.modelsNum = id2data.size();
        this.outputProgress = outputProgress;
    }

    /**
     * Passes all models from the directory associated with this analyzer to
     * the given visitor for analysis.
     *
     * @param visitor Visitor to use for analyzing models.
     */
    public void analyzeModels(ModelVisitor visitor) {
        int modelsAnalyzed = 0;
        for (File file : getModelsFiles()) {
            modelsAnalyzed = analyzeModels(file, visitor, modelsAnalyzed);
        }
    }

    /**
     * Passes all models from the given file to the given visitor for analysis.
     *
     * @param file           File to analyze models from.
     * @param visitor        Visitor to use for analyzing models.
     * @param modelsAnalyzed Number of models that were so far analyzed.
     * @returns Number of models that were so far analyzed.
     */
    private int analyzeModels(File file, ModelVisitor visitor, int modelsAnalyzed) {
        try {
            BufferedInputStream fileInput = new BufferedInputStream(
                    new FileInputStream(file));
            ObjectInputStream objectInput =
                    new ObjectInputStream(fileInput);
            int num = objectInput.readInt();
            for (int i = 0; i < num; i++) {
                int id = objectInput.readInt();
                Model model = (Model) objectInput.readObject();
                ModelData data = this.id2data.get(id);
                visitor.visit(id, model, data);
                if (outputProgress) {
                    int lastPercent = 100 * modelsAnalyzed / this.modelsNum;
                    modelsAnalyzed++;
                    int percent = 100 * modelsAnalyzed / this.modelsNum;
                    if (percent != lastPercent) {
                        System.out.println("Analyzed " + modelsAnalyzed + "/" +
                                this.modelsNum + " models (" + percent + "%)");
                    }
                }
            }
            objectInput.close();
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] This should never happen");
            e.printStackTrace(System.err);
            System.exit(0);
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR] File not found: " + file);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }
        return modelsAnalyzed;
    }

    /**
     * Gets all files from the given directory that contain models.
     *
     * @param dir Directory with models' files.
     * @return Files from the given directory that contain models.
     */
    private Set<File> getModelsFiles() {
        Set<File> result = new HashSet<File>();
        for (File file : this.modelsDir.listFiles()) {
            if (file.getName().endsWith(".models.ser")) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Reads data about models.
     */
    private Map<Integer, ModelData> readId2ModelData() {
        Map<Integer, ModelData> id2data = new HashMap<Integer, ModelData>();
        String fileName = "modelsdata.ser";
        try {
            BufferedInputStream fileInput = new BufferedInputStream(
                    new FileInputStream(new File(this.modelsDir, fileName)));
            ObjectInputStream objectInput =
                    new ObjectInputStream(fileInput);
            int num = objectInput.readInt();
            for (int i = 0; i < num; i++) {
                int id = objectInput.readInt();
                ModelData modelData = (ModelData) objectInput.readObject();
                id2data.put(id, modelData);
            }
            objectInput.close();
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] This should never happen");
            e.printStackTrace(System.err);
            System.exit(0);
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR] Directory " + this.modelsDir + " does " +
                    "not contain file " + fileName);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }
        return id2data;
    }
}
