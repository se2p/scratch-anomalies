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
package de.uni_passau.fim.se2.litterbox.ast.visitor;

import de.uni_passau.fim.se2.litterbox.ast.model.ASTNode;
import de.uni_passau.fim.se2.litterbox.ast.model.ActorDefinition;
import de.uni_passau.fim.se2.litterbox.ast.model.Program;
import de.uni_passau.fim.se2.litterbox.ast.model.Script;
import de.uni_passau.fim.se2.litterbox.ast.model.event.Event;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.Stmt;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.control.ControlStmt;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.control.IfElseStmt;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.control.IfThenStmt;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.control.RepeatForeverStmt;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.control.RepeatTimesStmt;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.control.UntilStmt;
import org.softevo.oumextractor.modelcreator1.ModelData;
import org.softevo.oumextractor.modelcreator1.model.EpsilonTransition;
import org.softevo.oumextractor.modelcreator1.model.InvokeMethodTransition;
import org.softevo.oumextractor.modelcreator1.model.MethodCall;
import org.softevo.oumextractor.modelcreator1.model.Model;
import org.softevo.oumextractor.modelcreator1.model.State;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Visitor used for creating actor usage models of scratch programs.
 */
public class AUMVisitor implements ScratchVisitor {

    /**
     * Logger to be used by this class.
     */
    private final static Logger logger =
            Logger.getLogger(AUMVisitor.class.getName());

    static {
        AUMVisitor.logger.setLevel(Level.ALL);
    }

    /**
     * Constant used as class name for every actor analysed.
     */
    private static final String ACTOR = "actor";

    /**
     * Constant used for dummy script naming. TODO find more sophisticated solution
     */
    private static final String SCRIPT = "script";

    /**
     * Constant used for naming the models. TODO is that correct at all
     */
    private static final String MODEL = "model";

    /**
     * Directory to store the models into.
     */
    private String pathToOutputDir;

    /**
     * Names of all programs to be analysed by this visitor.
     * TODO maybe change this to be automatically inferred
     */
    private final Set<String> programs; // typesnames

    /**
     * Mapping model id number => model data.
     */
    private final Map<Integer, ModelData> id2modelData;

    /**
     * Mapping model name => model for models of the currently processed method.
     */
    private final Map<String, Model> currentModels;

    /**
     * Model of the currently processed script.
     */
    private Model currentModel;

    /**
     * Models to serialize during next serialization.
     */
    private final Set<Model> modelsToSerialize;

    /**
     * Mapping model => model id.
     */
    private final Map<Model, Integer> model2id;

    /**
     * Number of models created by this analyzer. Used to generate model id.
     * Is the same as the amount of scripts analysed.
     */
    private int modelsCreated;

    /**
     * Name of the program currently analysed.
     */
    private String program;

    //TODO comment
    private State from;
    private State to;

    /**
     * Creates a new instance of this visitor.
     *
     * @param pathToOutputDir Directory to hold the models.
     * @param programs        Names of all programs that will be processed. TODO maybe automatically infer this
     */
    public AUMVisitor(String pathToOutputDir, Set<String> programs) {
        this.pathToOutputDir = pathToOutputDir;
        this.programs = new HashSet<String>(programs);
        this.id2modelData = new HashMap<Integer, ModelData>();
        this.model2id = new HashMap<Model, Integer>();
        this.currentModels = new HashMap<String, Model>();
        this.modelsToSerialize = new HashSet<Model>();
        this.modelsCreated = 0;
        this.currentModel = new Model();

        // empty models dir
        File destDir = new File(this.pathToOutputDir);
        destDir.mkdirs();
        for (File file : destDir.listFiles()) {
            file.delete();
        }
    }

    /**
     * Serializes models created since the last serialization (or since the
     * beginning of the analysis, if this is the first serialization) until
     * this point to a file.  This must be called after finishing analysis of
     * every class, because models are serialized in files according to classes,
     * from which they were created.
     */
    public void serialiseModels() {
        // serialize models if necessary
        if (this.modelsToSerialize.size() > 0) {
            File targetDirectory = new File(this.pathToOutputDir);
            try {
                String fileName = pathToOutputDir + "/" + program + ".models.ser";
                File file = new File(fileName);
                boolean newFile = file.createNewFile();
                BufferedOutputStream fileOutput = new BufferedOutputStream(
                        new FileOutputStream(new File(fileName), true));
                ObjectOutputStream objectOutput =
                        new ObjectOutputStream(fileOutput);
                objectOutput.writeInt(this.modelsToSerialize.size());
                for (Model model : this.modelsToSerialize) {
                    //model.minimize(); TODO do I need this
                    System.out.println(model);
                    objectOutput.writeInt(this.model2id.get(model));
                    objectOutput.writeObject(model);
                }
                objectOutput.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
                System.exit(0);
            }
        }

        // empty the set of models to be serialized
        this.modelsToSerialize.clear();
        this.model2id.clear();
    }

    /**
     * Called after analysis of a script is done.
     */
    public void endScriptAnalysis(Model currentModel) {
        this.modelsCreated++;
        this.model2id.put(currentModel, this.modelsCreated);
        this.currentModels.put(MODEL + modelsCreated, currentModel); //TODO this is a quickfix
        this.modelsToSerialize.add(currentModel); //FIXME quickfix as well
        //TODO check whether the following line is correct or not
        this.id2modelData.put(this.modelsCreated, new ModelData(ACTOR, SCRIPT + modelsCreated + "()V", MODEL + modelsCreated)); //TODO I am not sure
        // TODO whether it is correct to name every class the same here or not. Maybe this is not the right place to do so
    }

    /**
     * Serialises the info collected.
     */
    public void shutdownAnalysis() {
        // serialise models info
        File targetDirectory = new File(this.pathToOutputDir);
        targetDirectory.mkdirs(); //TODO probably unnecessary here
        try {
            String fileName = "modelsdata.ser";
            BufferedOutputStream fileOutput = new BufferedOutputStream(
                    new FileOutputStream(new File(targetDirectory, fileName)));
            ObjectOutputStream objectOutput =
                    new ObjectOutputStream(fileOutput);
            writeId2ModelData(objectOutput);
            objectOutput.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }

        // serialise names of types investigated
        try {
            String fileName = "typesnames.ser";
            BufferedOutputStream fileOutput = new BufferedOutputStream(
                    new FileOutputStream(new File(targetDirectory, fileName)));
            ObjectOutputStream objectOutput =
                    new ObjectOutputStream(fileOutput);
            writePrograms(objectOutput);
            objectOutput.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }

        // create index file
        PrintStream ps = null;
        try {
            ps = new PrintStream(new File(targetDirectory, "index.txt"));
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Couldn't create index.txt in " +
                    targetDirectory);
            return;
        }

        // fill the index
        List<Integer> ids =
                new ArrayList<Integer>(this.id2modelData.keySet());
        System.out.println(ids.size() + " MODELS EXTRACTED");
        Collections.sort(ids);
        for (Integer id : ids) {
            ModelData modelData = this.id2modelData.get(id);

            StringBuffer description = new StringBuffer();
            description.append("INDEX:  ").append(id).append("\n");
            description.append("MODEL:  ").append(modelData.getModelName());
            description.append("\n");
            description.append("CLASS:  ").append(modelData.getClassName());
            description.append("\n");
            description.append("METHOD: ").append(modelData.getMethodName());
            description.append("\n");
            ps.println("--------------------------------------------------");
            ps.print(description);
            ps.println("--------------------------------------------------");
            ps.println();
        }
        ps.close();
    }

    /**
     * Serializes list of types analyzed by this Analyzer into given stream.
     *
     * @param out Stream to serialize the types' names to.
     * @throws IOException Thrown by writeObject in case there are problems
     *                     with the underlying stream.
     */
    private void writePrograms(ObjectOutputStream out) throws IOException {
        out.writeInt(this.programs.size());
        for (String program : this.programs) {
            out.writeObject(program);
        }
    }

    /**
     * Serializes id2modelData field of the analyzer into given stream.
     *
     * @param out Stream to serialize the field to.
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream.
     */
    private void writeId2ModelData(ObjectOutputStream out) throws IOException {
        out.writeInt(this.id2modelData.size());
        for (Integer id : this.id2modelData.keySet()) {
            ModelData modelData = this.id2modelData.get(id);
            out.writeInt(id);
            out.writeObject(modelData);
        }
    }

    /**
     * Called when analysis of a script produces results in an exception.
     */
    public void rollbackAnalysis(Model currentModel) {
        this.modelsToSerialize.remove(currentModel);
        this.model2id.remove(currentModel);
        currentModel.clear();
    }

    /**
     * Creates an actor usage model for this program.
     *
     * @param program The program of which the actor usage model is to be created.
     */
    @Override
    public void visit(Program program) {
        this.program = program.getIdent().getName();
        System.out.println(program.getIdent().getName());
        for (ActorDefinition definition : program.getActorDefinitionList().getDefintions()) {
            for (Script script : definition.getScripts().getScriptList()) {
                script.accept(this);
            }
        }
        serialiseModels();
    }

    /**
     * Does the magic. Work in progress. TODO
     *
     * @param script A script of an actor. TODO
     */
    @Override
    public void visit(Script script) {
        System.out.println("Script: " + script.getUniqueName());
        currentModel = new Model();
        script.getEvent().accept(this);
        for (Stmt stmt : script.getStmtList().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        EpsilonTransition returnTransition = EpsilonTransition.get();
        from = to;
        to = currentModel.getExitState();
        currentModel.addTransition(from, to, returnTransition);
        Model toAdd = new Model(currentModel);
        modelsToSerialize.add(toAdd); //FIXME
        endScriptAnalysis(toAdd);//FIXME
        currentModel = new Model();
    }

    @Override
    public void visit(Event event) {
        System.out.println("Event: " + event.getUniqueName());
        from = currentModel.getEntryState();
        addTransition(from, event.getUniqueName());
    }

    @Override
    public void visit(Stmt stmt) {
        System.out.println("stmt: " + stmt.getUniqueName());
        System.out.println("from: " + from);
        System.out.println("to: " + to);
        from = to;
        addTransition(from, stmt.getUniqueName());
    }

    @Override
    public void visit(RepeatForeverStmt repeatForeverStmt)  {
        //TODO
        from = to;
        addTransition(from, repeatForeverStmt.getUniqueName());
    }

    @Override
    public void visit(IfElseStmt ifElseStmt) {
        //TODO
        from = to;
        addTransition(from, ifElseStmt.getUniqueName());
    }

    @Override
    public void visit(IfThenStmt ifThenStmt) {
        //TODO
        from = to;
        addTransition(from, ifThenStmt.getUniqueName());
    }

    @Override
    public void visit(RepeatTimesStmt repeatTimesStmt) {
        //TODO
        from = to;
        addTransition(from, repeatTimesStmt.getUniqueName());
    }

    @Override
    public void visit(UntilStmt untilStmt) {
        //TODO
        from = to;
        addTransition(from, untilStmt.getUniqueName());
    }


    private void addTransition(State from, String uniqueName) {
        MethodCall methodCall = new MethodCall("sprite", uniqueName);
        InvokeMethodTransition transition = InvokeMethodTransition.get(methodCall, new ArrayList<>());
        State followUpState = currentModel.getFollowUpState(from, transition);
        to = followUpState;
        currentModel.addTransition(from, followUpState, transition);
    }
}
