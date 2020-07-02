package de.uni_passau.fim.se2.litterbox.analytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.softevo.oumextractor.modelcreator1.ModelData;
import org.softevo.oumextractor.modelcreator1.model.Model;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AUMExtractorTest {

    @Test
    public void testFileCreation(@TempDir File tempDir) throws Exception {
        AUMExtractor extractor = new AUMExtractor("src/test/fixtures/aums/", null, tempDir.toString());
        extractor.runAnalysis();

        File typesnames = new File(tempDir, "typesnames.ser");
        File modelsdata = new File(tempDir, "modelsdata.ser");
        File summary = new File(tempDir, "summary.txt");
        File index = new File(tempDir, "index.txt");
        File exceptions = new File(tempDir, "exceptions.txt");

        assertTrue(typesnames.exists());
        assertTrue(modelsdata.exists());
        assertTrue(summary.exists());
        assertTrue(index.exists());
        assertTrue(exceptions.exists());
    }

    @Test
    public void testTerminatedForeverModel(@TempDir File tempDir) throws Exception {
        AUMExtractor extractor = new AUMExtractor("src/test/fixtures/aums/terminatedForever", null, tempDir.toString());
        extractor.runAnalysis();

        Map<Integer, ModelData> id2data = getId2ModelData(tempDir);
        List<Model> models = getModels(tempDir, id2data);
        assertEquals(1, models.size());
        Model model = models.get(0);
        assertEquals(6, model.getAllTransitions().size());
        assertEquals(6, model.getUnderlyingGraph().getVertices().size());
        assertTrue(model.toString().contains("\"ENTRY\" --KeyPressed--> AS 2"));
        assertTrue(model.toString().contains("AS 2 --IfThenStmt--> AS 3"));
        assertTrue(model.toString().contains("AS 3 --MoveSteps--> \"EXIT\""));
        assertTrue(model.toString().contains("AS 3 --RepeatForeverStmt--> AS 5"));
        assertTrue(model.toString().contains("AS 5 --TurnRight--> AS 6"));
        assertTrue(model.toString().contains("AS 6 --StopAll--> \"EXIT\""));
    }

    private Map<Integer, ModelData> getId2ModelData(File tempDir) throws IOException, ClassNotFoundException {
        Map<Integer, ModelData> id2data = new HashMap<Integer, ModelData>();
        String fileName = "modelsdata.ser";
        BufferedInputStream fileInput = new BufferedInputStream(
                new FileInputStream(new File(tempDir, fileName)));
        ObjectInputStream objectInput =
                new ObjectInputStream(fileInput);
        int num = objectInput.readInt();
        for (int i = 0; i < num; i++) {
            int id = objectInput.readInt();
            ModelData modelData = (ModelData) objectInput.readObject();
            id2data.put(id, modelData);
        }
        objectInput.close();
        return id2data;
    }

    private List<Model> getModels(File tempDir, Map<Integer, ModelData> id2data) throws IOException, ClassNotFoundException {
        List<Model> models = new LinkedList<>();
        for (File file : tempDir.listFiles()) {
            if (file.getName().endsWith(".models.ser")) {

                BufferedInputStream fileInput = new BufferedInputStream(
                        new FileInputStream(file));
                ObjectInputStream objectInput =
                        new ObjectInputStream(fileInput);
                int number = objectInput.readInt();
                for (int i = 0; i < number; i++) {
                    int id = objectInput.readInt(); // do not delete this, even if you don't need the model data. Otherwise this process will fail.
                    Model model = (Model) objectInput.readObject();
                    ModelData data = id2data.get(id);
                    models.add(model);
                }
                objectInput.close();
            }
        }
        return models;
    }
}