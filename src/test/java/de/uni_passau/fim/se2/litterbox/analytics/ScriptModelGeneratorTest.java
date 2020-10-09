/*
 * Copyright (C) 2020 LitterBox contributors
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

public class ScriptModelGeneratorTest {

    @Test
    public void testFileCreation(@TempDir File tempDir) throws Exception {
        ScriptModelGenerator extractor = new ScriptModelGenerator("src/test/fixtures/aums/", null, tempDir.toString());
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
        ScriptModelGenerator extractor = new ScriptModelGenerator("src/test/fixtures/aums/terminatedForever", null, tempDir.toString());
        extractor.runAnalysis();

        Map<Integer, ModelData> id2data = getId2ModelData(tempDir);
        List<Model> models = getModels(tempDir, id2data);
        assertEquals(1, models.size());
        Model model = models.get(0);
        assertEquals(6, model.getAllTransitions().size());
        assertEquals(6, model.getUnderlyingGraph().getVertices().size());
        String modelString = model.toString();
        assertTrue(modelString.contains("\"ENTRY\" --KeyPressed--> AS 2"));
        assertTrue(modelString.contains("AS 2 --IfThenStmt--> AS 3"));
        assertTrue(modelString.contains("AS 3 --MoveSteps--> \"EXIT\""));
        assertTrue(modelString.contains("AS 3 --RepeatForeverStmt--> AS 5"));
        assertTrue(modelString.contains("AS 5 --TurnRight--> AS 6"));
        assertTrue(modelString.contains("AS 6 --StopAll--> \"EXIT\""));
    }

    @Test
    public void testForeverInProcedure(@TempDir File tempDir) throws Exception {
        ScriptModelGenerator extractor = new ScriptModelGenerator("src/test/fixtures/aums/foreverInProcedure", null, tempDir.toString());
        extractor.runAnalysis();

        Map<Integer, ModelData> id2ModelData = getId2ModelData(tempDir);
        List<Model> models = getModels(tempDir, id2ModelData);
        assertEquals(1, models.size());
        Model model = models.get(0);
        assertEquals(4, model.getAllTransitions().size());
        assertEquals(4, model.getUnderlyingGraph().getVertices().size());
        String modelString = model.toString();
        assertTrue(modelString.contains("\"ENTRY\" --ProcedureDefinition--> AS 2"));
        assertTrue(modelString.contains("AS 2 --SayForSecs--> AS 3"));
        assertTrue(modelString.contains("AS 3 --RepeatForeverStmt--> AS 4"));
        assertTrue(modelString.contains("AS 4 --Say--> AS 4"));
    }

    @Test
    public void testControlStmts(@TempDir File tempDir) throws Exception {
        ScriptModelGenerator extractor = new ScriptModelGenerator("src/test/fixtures/aums/controlStmts/", null, tempDir.toString());
        extractor.runAnalysis();

        Map<Integer, ModelData> id2ModelData = getId2ModelData(tempDir);
        List<Model> models = getModels(tempDir, id2ModelData);
        assertEquals(1, models.size());
        Model model = models.get(0);
        assertEquals(12, model.getAllTransitions().size());
        assertEquals(9, model.getUnderlyingGraph().getVertices().size());
        String modelString = model.toString();
        assertTrue(modelString.contains("\"ENTRY\" --GreenFlag--> AS 2"));
        assertTrue(modelString.contains("AS 2 --MoveSteps--> AS 3"));
        assertTrue(modelString.contains("AS 3 --RepeatTimesStmt--> AS 4"));
        assertTrue(modelString.contains("AS 4 --IfThenStmt--> AS 5"));
        assertTrue(modelString.contains("AS 4 --IfElseStmt--> AS 9"));
        assertTrue(modelString.contains("AS 5 ----> AS 4"));
        assertTrue(modelString.contains("AS 5 --TurnRight--> AS 4"));
        assertTrue(modelString.contains("AS 14 --StopAllSounds--> AS 14"));
        assertTrue(modelString.contains("AS 11 --AskAndWait--> \"EXIT\""));
        assertTrue(modelString.contains("AS 11 --ClearSoundEffects--> AS 11"));
        assertTrue(modelString.contains("AS 9 --RepeatForeverStmt--> AS 14"));
        assertTrue(modelString.contains("AS 9 --UntilStmt--> AS 11"));
    }

    @Test
    public void testDotfileGenerationNoSeparator(@TempDir File tempDir) throws Exception {
        ScriptModelGenerator extractor = new ScriptModelGenerator("src/test/fixtures/aums/foreverInProcedure", tempDir.toString(), tempDir.toString());
        extractor.runAnalysis();
        File dotfile = new File(tempDir, "script model of program foreverInProcedure.json actor Sprite1 procedure define test.dot");
        assertTrue(dotfile.exists());
    }

    @Test
    public void testDotfileGenerationSeparatorPresent(@TempDir File tempDir) throws Exception {
        ScriptModelGenerator extractor = new ScriptModelGenerator("src/test/fixtures/aums/foreverInProcedure", tempDir.toString() + File.separator, tempDir.toString() + File.separator);
        extractor.runAnalysis();
        File dotfile = new File(tempDir, "script model of program foreverInProcedure.json actor Sprite1 procedure define test.dot");
        assertTrue(dotfile.exists());
    }

    private Map<Integer, ModelData> getId2ModelData(File modelsDir) throws IOException, ClassNotFoundException {
        Map<Integer, ModelData> id2data = new HashMap<>();
        String fileName = "modelsdata.ser";
        BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(new File(modelsDir, fileName)));
        ObjectInputStream objectInput = new ObjectInputStream(fileInput);
        int num = objectInput.readInt();
        for (int i = 0; i < num; i++) {
            int id = objectInput.readInt();
            ModelData modelData = (ModelData) objectInput.readObject();
            id2data.put(id, modelData);
        }
        objectInput.close();
        return id2data;
    }

    private List<Model> getModels(File modelsDir, Map<Integer, ModelData> id2data) throws IOException, ClassNotFoundException {
        List<Model> models = new LinkedList<>();
        for (File file : modelsDir.listFiles()) {
            if (file.getName().endsWith(".models.ser")) {
                BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(file));
                ObjectInputStream objectInput = new ObjectInputStream(fileInput);
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
