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

import de.uni_passau.fim.se2.litterbox.analytics.AUMExtractor;
import de.uni_passau.fim.se2.litterbox.ast.model.ActorDefinition;
import de.uni_passau.fim.se2.litterbox.ast.model.Program;
import de.uni_passau.fim.se2.litterbox.ast.model.Script;
import de.uni_passau.fim.se2.litterbox.ast.model.StmtList;
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
     * Directory to store the models into.
     */
    private String pathToOutputDir;

    /**
     * Names of all programs to be analysed by this visitor.
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
     * Name of the program currently analysed.
     */
    private String programName;

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
    private List<State> states = new LinkedList<>(); //TODO introduce HashMap

    /**
     * The state which will be used as starting edge for the next transition.
     */
    private State transitionStart;

    /**
     * The state which will be used as end point for the next transition.
     */
    private State transitionEnd;

    /**
     * Creates a new instance of this visitor.
     *
     * @param pathToOutputDir Directory to hold the models.
     * @param programs        Names of all programs that will be processed.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public AUMVisitor(String pathToOutputDir, Set<String> programs) {
        this.pathToOutputDir = pathToOutputDir;
        this.programs = programs;
        id2modelData = new HashMap<>();
        model2id = new HashMap<>();
        modelsToSerialize = new HashSet<>();
        modelsCreated = 0;
        currentModel = new Model();
        currentActorName = "";

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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void serialiseModels() {
        // serialize models if necessary
        if (modelsToSerialize.size() > 0) {
            try {
                String fileName = pathToOutputDir + "/" + programName + ".models.ser";
                File file = new File(fileName);
                file.createNewFile();
                BufferedOutputStream fileOutput = new BufferedOutputStream(
                        new FileOutputStream(new File(fileName), true));
                ObjectOutputStream objectOutput =
                        new ObjectOutputStream(fileOutput);
                objectOutput.writeInt(modelsToSerialize.size());
                for (Model model : modelsToSerialize) {
                    //model.minimize(); TODO do I need this
                    objectOutput.writeInt(model2id.get(model));
                    objectOutput.writeObject(model);
                }
                objectOutput.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
                System.exit(0);
            }
        }

        // empty the set of models to be serialized
        modelsToSerialize.clear();
        model2id.clear();
    }

    /**
     * Called after analysis of a script is done.
     */
    public void endScriptAnalysis(Model toAdd) {
        AUMExtractor.newScriptAnalysed();
        for (int i = 0; i < states.size(); i++) {
            assert (i + 1 == states.get(i).getId());
        }
        modelsCreated++;
        modelsToSerialize.add(toAdd);
        model2id.put(toAdd, modelsCreated);
        id2modelData.put(modelsCreated, new ModelData(
                programName + "." + currentActorName,
                SCRIPT + modelsToSerialize.size() + "()V", ACTOR));
        clear();
    }

    /**
     * Clears the states of the visitor which are dependent on the current
     * script which is analysed.
     */
    private void clear() {
        states.clear();
        setTransitionStartTo(null);
        transitionEnd = null;
        currentModel = new Model();
    }

    /**
     * Serialises the info collected.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void shutdownAnalysis() {
        // serialise models info
        File targetDirectory = new File(pathToOutputDir);
        targetDirectory.mkdirs();
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
        PrintStream ps;
        try {
            ps = new PrintStream(new File(targetDirectory, "index.txt"));
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Couldn't create index.txt in " +
                    targetDirectory);
            return;
        }

        // fill the index
        List<Integer> ids =
                new ArrayList<>(id2modelData.keySet());
        System.out.println(ids.size() + " MODELS EXTRACTED");
        Collections.sort(ids);
        for (Integer id : ids) {
            ModelData modelData = id2modelData.get(id);

            ps.println("--------------------------------------------------");
            String description = "INDEX:  " + id + "\n"
                    + "MODEL:  " + modelData.getModelName()
                    + "\n"
                    + "CLASS:  " + modelData.getClassName()
                    + "\n"
                    + "METHOD: " + modelData.getMethodName()
                    + "\n";
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
        out.writeInt(id2modelData.size());
        for (Integer id : id2modelData.keySet()) {
            ModelData modelData = id2modelData.get(id);
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
        out.writeInt(programs.size());
        for (String program : programs) {
            out.writeObject(program);
        }
    }

    /**
     * Called when analysis of a script produces results in an exception.
     */
    public void rollbackAnalysis(Model currentModel) {
        clear();
        currentActorName = "";
        if (currentModel != null) {
            modelsToSerialize.remove(currentModel);
            model2id.remove(currentModel);
        }
    }

    /**
     * Updates the present state for the next transition to {@code to}.
     *
     * @param startOfTransition The state to which the next transition will be
     *                          added.
     */
    private void setTransitionStartTo(State startOfTransition) {
        transitionStart = startOfTransition;
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
        AUMExtractor.newProjectPresent();
        programName = program.getIdent().getName();
        for (ActorDefinition definition : program.getActorDefinitionList().getDefintions()) {
            currentActorName = definition.getIdent().getName();
            for (Script script : definition.getScripts().getScriptList()) {
                script.accept(this);
            }
            currentActorName = "";
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
        AUMExtractor.newScriptPresent();
        script.getEvent().accept(this);
        for (Stmt stmt : script.getStmtList().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        EpsilonTransition returnTransition = EpsilonTransition.get();
        setTransitionStartTo(transitionEnd);
        transitionEnd = currentModel.getExitState();
        currentModel.addTransition(transitionStart, transitionEnd, returnTransition);
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
        setTransitionStartTo(currentModel.getEntryState());
        states.add(transitionStart);
        addTransition(transitionStart, event.getUniqueName());
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
     * @param loop The statement causing the transition.
     */
    @Override
    public void visit(RepeatForeverStmt loop) {
        addLoopTransitions(loop.getUniqueName(), loop.getStmtList());
    }

    /***
     * Adds a transition for the loop and adds transitions for its statement
     * list.
     *
     * @param loop The statement causing the transition.
     */
    @Override
    public void visit(RepeatTimesStmt loop) {
        addLoopTransitions(loop.getUniqueName(), loop.getStmtList());
    }

    /**
     * Adds a transition for the loop and adds transitions for its statement
     * list.
     *
     * @param loop The statement causing the transition.
     */
    @Override
    public void visit(UntilStmt loop) {
        addLoopTransitions(loop.getUniqueName(), loop.getStmtList());
    }

    /**
     * Updates the actor usage model to contain all transitions related to this
     * loop, including its contained statements and the epsilon transition at
     * the end.
     *
     * @param stmtName Name of the loop causing this transition.
     * @param stmtList List of statements contained in this loop.
     */
    private void addLoopTransitions(String stmtName, StmtList stmtList) {
        addTransitionContextAware(stmtName);
        int repeatStateIndex = transitionEnd.getId();
        List<Stmt> listOfStmt = stmtList.getStmts().getListOfStmt();
        for (Stmt stmt : listOfStmt) {
            stmt.accept(this);
        }
        setTransitionStartTo(transitionEnd);
        EpsilonTransition transition = EpsilonTransition.get();
        State repeatState = states.get(repeatStateIndex - 1);
        currentModel.addTransition(transitionStart, repeatState, transition);
        transitionEnd = repeatState;
    }

    /**
     * Adds a transition for the if-statement itself, and for its true and false
     * branches and the statements in the branches.
     *
     * @param ifElseStmt The statement causing the transition.
     */
    @Override
    public void visit(IfElseStmt ifElseStmt) {
        // ad control stmt
        int afterIfStmtIndex = addTrueBranch(ifElseStmt.getUniqueName());
        // add true branch stmts
        for (Stmt stmt : ifElseStmt.getStmtList().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        // save index of last state in true branch
        int endOfTrueBranchStateId = transitionEnd.getId();
        // add epsilon transition for the false branch
        transitionStart = states.get(afterIfStmtIndex - 1);
        EpsilonTransition trans = EpsilonTransition.get();
        State follow = currentModel.getNewState();
        states.add(follow);
        transitionEnd = follow;
        currentModel.addTransition(transitionStart, follow, trans);
        // add false branch stmts
        for (Stmt stmt : ifElseStmt.getElseStmts().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        // the next state currently is the end of the false branch
        setTransitionStartTo(transitionEnd);
        joinBranches(endOfTrueBranchStateId);
    }

    /**
     * Adds a transition for the if-statement itself, and for its true and false
     * branches and the statements in the true branch.
     *
     * @param ifThenStmt The statement causing the transition.
     */
    @Override
    public void visit(IfThenStmt ifThenStmt) {
        // add control stmt
        int afterIfStmtIndex = addTrueBranch(ifThenStmt.getUniqueName());
        // add true branch stmts
        for (Stmt stmt : ifThenStmt.getThenStmts().getStmts().getListOfStmt()) {
            stmt.accept(this);
        }
        int endOfTrueBranchStateId = transitionEnd.getId();
        // the end of the true branch is the state after the if stmt
        setTransitionStartTo(states.get(afterIfStmtIndex - 1));
        joinBranches(endOfTrueBranchStateId);
    }

    /**
     * Joins the two branches to end in one state by adding the state and
     * epsilon transitions to it.
     * The present state has to be set to the end of the false branch before
     * calling this method.
     *
     * @param endOfTrueBranchStateId Id of the state at the end of the true
     *                               branch.
     */
    private void joinBranches(int endOfTrueBranchStateId) {
        State join = currentModel.getNewState();
        states.add(join);
        EpsilonTransition falseTransition = EpsilonTransition.get();
        currentModel.addTransition(transitionStart, join, falseTransition);
        // add epsilon transition after true branch
        State endOfTrueBranch = states.get(endOfTrueBranchStateId - 1);
        setTransitionStartTo(endOfTrueBranch);
        EpsilonTransition trueTransition = EpsilonTransition.get();
        currentModel.addTransition(transitionStart, join, trueTransition);
        transitionEnd = join;
    }

    /**
     * Adds the if statement and the epsilon transition for the true branch to
     * the current model.
     *
     * @param ifName Name of the if statement.
     * @return The id of the state after the if statement.
     */
    private int addTrueBranch(String ifName) {
        setTransitionStartTo(transitionEnd);
        addTransition(transitionStart, ifName);
        // save index of state after control stmt
        int afterIfStmtIndex = transitionEnd.getId();
        // add epsilon transition for true branch
        setTransitionStartTo(transitionEnd);
        EpsilonTransition transition = EpsilonTransition.get();
        State followUpState = currentModel.getFollowUpState(transitionStart, transition);
        states.add(followUpState);
        currentModel.addTransition(transitionStart, followUpState, transition);
        transitionEnd = followUpState;
        return afterIfStmtIndex;
    }

    /**
     * Adds a transition being aware of the context so that loops back to
     * control statements are made.
     *
     * @param stmtName Name of the statement causing this transition.
     */
    private void addTransitionContextAware(String stmtName) {
        setTransitionStartTo(transitionEnd);
        addTransition(transitionStart, stmtName);
    }

    /**
     * Adds a transition from the given state to {@code this.nextState} to the
     * current model.
     *
     * @param presentState The starting state of the transition added.
     * @param stmtName     The name of the block causing the transition.
     */
    private void addTransition(State presentState, String stmtName) {
        MethodCall methodCall = new MethodCall(ACTOR, stmtName);
        InvokeMethodTransition transition = InvokeMethodTransition.get(methodCall, new ArrayList<>());
        State followUpState = currentModel.getFollowUpState(presentState, transition);
        states.add(followUpState);
        transitionEnd = followUpState;
        currentModel.addTransition(presentState, followUpState, transition);
    }
}