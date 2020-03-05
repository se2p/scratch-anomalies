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

import de.uni_passau.fim.se2.litterbox.ast.model.ActorDefinition;
import de.uni_passau.fim.se2.litterbox.ast.model.Program;
import de.uni_passau.fim.se2.litterbox.ast.model.Script;
import de.uni_passau.fim.se2.litterbox.ast.model.event.Event;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.Stmt;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
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
     * Constant used for actor naming.
     */
    private static final String ACTOR = "actor";

    /**
     * Constant used for script naming.
     */
    private static final String SCRIPT = "script";

    /**
     * Constant used for model naming.
     */
    private static final String MODEL = "model";

    /**
     * Name of transitions modeling the true branch of control statements.
     */
    private static final String TRUE = "true";

    /**
     * Name of transitions modeling the false branch of control statements.
     */
    private static final String FALSE = "false";

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
     * Models to serialize during next serialization.
     */
    private final Set<Model> modelsToSerialize;


    /**
     * Mapping model id number => model data.
     */
    private final Map<Integer, ModelData> id2modelData;

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
     * Number of total actors analysed so far.
     */
    private int actorsAnalysed;

    /**
     * Name of the program currently analysed.
     */
    private String program;

    /**
     * Name of the current actor the scripts of which are analysed currently.
     */
    private String currentActorName;

    /**
     * Model of the currently processed script.
     */
    private Model currentModel;

    /**
     * List of all states produced per model.
     */
    private List<State> states = new LinkedList<>();

    /**
     * The state which will be used as starting edge for the next transition.
     */
    private State presentState;

    /**
     * The state which will be used as end point for the next transition.
     */
    private State nextState;

    /**
     * Stack holding the states of control statements that have to be looped
     * back to.
     */
    private Stack<State> controlStmtStates;

    /**
     * Indicates that the next transition has to loop back to the state of a
     * control statement.
     */
    private boolean lastOfList = false;

    /**
     * Creates a new instance of this visitor.
     *
     * @param pathToOutputDir Directory to hold the models.
     * @param programs        Names of all programs that will be processed. TODO maybe automatically infer this
     */
    public AUMVisitor(String pathToOutputDir, Set<String> programs) {
        this.pathToOutputDir = pathToOutputDir;
        this.programs = programs;
        this.id2modelData = new HashMap<Integer, ModelData>();
        this.model2id = new HashMap<Model, Integer>();
        this.modelsToSerialize = new HashSet<Model>();
        this.modelsCreated = 0;
        this.currentModel = new Model();
        this.actorsAnalysed = 0;
        this.currentActorName = "";
        this.controlStmtStates = new Stack<>();

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
    public void endScriptAnalysis(Model toAdd) {
        this.modelsCreated++;
        this.modelsToSerialize.add(toAdd);
        this.model2id.put(toAdd, this.modelsCreated);
        this.id2modelData.put(this.modelsCreated, new ModelData(program + "." + currentActorName + actorsAnalysed, SCRIPT + modelsCreated + "()V", MODEL + modelsCreated)); //TODO I am not sure
        // TODO whether it is correct to name every class the same here or not. Maybe this is not the right place to do so
        clear();
    }

    /**
     * Clears the states of the visitor which are dependent on the current
     * script which is analysed.
     */
    private void clear() {
        this.states.clear();
        updatePresentState(null);
        this.nextState = null;
        this.currentActorName = "";
        this.currentModel = new Model();
        this.lastOfList = false;
        this.controlStmtStates.clear();
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
     * Called when analysis of a script produces results in an exception.
     */
    public void rollbackAnalysis(Model currentModel) {
        clear();
        if (currentModel != null) {
            this.modelsToSerialize.remove(currentModel);
            this.model2id.remove(currentModel);
        }
    }

    /**
     * Updates the present state for the next transition to {@code to}.
     *
     * @param nextPresentState The state to which the next transition will be
     *                         added.
     */
    private void updatePresentState(State nextPresentState) {
        presentState = nextPresentState;
    }

    /**
     * Iterates over every actor definition of this program and lets every
     * script accept this visitor and serialises the models produced.
     * Avoids unnecessarily complex iteration by skipping the visit methods for
     * actor definitions.
     *
     * @param program The program scripts of which are to be analysed.
     */
    @Override
    public void visit(Program program) {
        this.program = program.getIdent().getName();
        for (ActorDefinition definition : program.getActorDefinitionList().getDefintions()) {
            actorsAnalysed++;
            currentActorName = definition.getIdent().getName();
            for (Script script : definition.getScripts().getScriptList()) {
                script.accept(this);
            }
        }
        serialiseModels();
    }

    /**
     * Creates an actor usage model of this script by iterating every statement.
     *
     * @param script The script of which an actor usage model is to be created.
     */
    @Override
    public void visit(Script script) {
        script.getEvent().accept(this);
        for (Stmt stmt : script.getStmtList().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        EpsilonTransition returnTransition = EpsilonTransition.get();
        updatePresentState(nextState);
        nextState = currentModel.getExitState();
        currentModel.addTransition(presentState, nextState, returnTransition);
        endScriptAnalysis(new Model(currentModel));
    }

    /**
     * Adds a transition for the event of the script and sets the present state
     * to the entry state of the model.
     *
     * @param event The event of the script currently analysed.
     */
    @Override
    public void visit(Event event) {
        updatePresentState(currentModel.getEntryState());
        states.add(presentState);
        addTransition(presentState, event.getUniqueName());
    }

    /**
     * Adds a transition for the statement to the current model.
     *
     * @param stmt The statement causing the transition.
     */
    @Override
    public void visit(Stmt stmt) {
        addTransitionContextAware(stmt.getUniqueName());
    }

    /***
     * Adds a transition for the loop and adds transitions for its statement
     * list.
     *
     * @param repeatForever The statement causing the transition.
     */
    @Override
    public void visit(RepeatForeverStmt repeatForever) {
        addTransitionContextAware(repeatForever.getUniqueName());
        int repeatStateIndex = getId(nextState);
        State repeatState = states.get(repeatStateIndex - 1);
        controlStmtStates.push(repeatState);
        List<Stmt> listOfStmt = repeatForever.getStmtList().getStmts().getListOfStmt();
        int size = listOfStmt.size();
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                lastOfList = true;
            }
            listOfStmt.get(i).accept(this);
        }
        this.nextState = repeatState;

    }

    @Override
    public void visit(IfElseStmt ifElseStmt) {
        updatePresentState(nextState);
        addTransition(presentState, ifElseStmt.getUniqueName());
        updatePresentState(nextState);
        int originalFromIndex = getId(presentState);
        addTransition(presentState, TRUE);
        for (Stmt stmt : ifElseStmt.getStmtList().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        State afterIf = states.get(originalFromIndex - 1);
        addTransition(afterIf, FALSE);
        updatePresentState(states.get(originalFromIndex - 1));
        for (Stmt stmt : ifElseStmt.getElseStmts().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        nextState = afterIf;
    }

    @Override
    public void visit(IfThenStmt ifThenStmt) {
        updatePresentState(nextState);
        addTransition(presentState, ifThenStmt.getUniqueName());
        updatePresentState(nextState);
        addTransition(presentState, TRUE);
        int originalFromIndex = getId(presentState);
        for (Stmt stmt : ifThenStmt.getThenStmts().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        nextState = states.get(originalFromIndex - 1);
    }

    @Override
    public void visit(RepeatTimesStmt repeatTimesStmt) {
        updatePresentState(nextState);
        addTransition(presentState, repeatTimesStmt.getUniqueName());
        int originalFromIndex = getId(presentState);
        for (Stmt stmt : repeatTimesStmt.getStmtList().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        nextState = states.get(originalFromIndex);
    }

    @Override
    public void visit(UntilStmt untilStmt) {
        updatePresentState(nextState);
        addTransition(presentState, untilStmt.getUniqueName());
        int originalFromIndex = getId(presentState);
        for (Stmt stmt : untilStmt.getStmtList().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        nextState = states.get(originalFromIndex);
    }

    /**
     * A workaround to retrieve the id of a state without changing the
     * OUMExtractor source code. TODO introduce a method to the source code.
     *
     * @param state The state of which the id is queried.
     * @return The id of the state.
     */
    private int getId(State state) {
        String s = state.toString();
        return Integer.parseInt(s.substring(s.length() - 1));
    }

    /**
     * Adds a transition being aware of the context so that loops back to
     * control statements are made.
     *
     * @param stmtName Name of the statement causing this transition.
     */
    private void addTransitionContextAware(String stmtName) {
        if (!lastOfList) {
            updatePresentState(nextState);
            addTransition(presentState, stmtName);
        } else {
            updatePresentState(nextState);
            MethodCall methodCall = new MethodCall(ACTOR + actorsAnalysed, stmtName); //TODO correctness?
            InvokeMethodTransition transition = InvokeMethodTransition.get(methodCall, new ArrayList<>());
            State controlStmtState = controlStmtStates.pop();
            currentModel.addTransition(presentState, controlStmtState, transition);
            nextState = controlStmtState;
            lastOfList = false;
        }
    }

    /**
     * Adds a transition from the given state to {@code this.nextState} to the
     * current model.
     *
     * @param presentState The starting state of the transition added.
     * @param stmtName     The name of the block causing the transition.
     */
    private void addTransition(State presentState, String stmtName) { //TODO from is probably always this.from, include from = to;?
        MethodCall methodCall = new MethodCall(ACTOR + actorsAnalysed, stmtName); //TODO correctness?
        InvokeMethodTransition transition = InvokeMethodTransition.get(methodCall, new ArrayList<>());
        State followUpState = currentModel.getFollowUpState(presentState, transition);
        states.add(followUpState);
        nextState = followUpState;
        currentModel.addTransition(presentState, followUpState, transition);
    }
}