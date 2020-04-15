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
import de.uni_passau.fim.se2.litterbox.ast.model.procedure.ProcedureDefinition;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.Stmt;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.control.*;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.termination.StopAll;
import de.uni_passau.fim.se2.litterbox.ast.model.statement.termination.StopThisScript;
import org.softevo.oumextractor.modelcreator1.ModelData;
import org.softevo.oumextractor.modelcreator1.model.*;

import java.io.*;
import java.util.*;
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
     * Constant used for procedure definition naming.
     */
    private static final String PROC_DEF = "procedure";

    /**
     * Directory to store the models into.
     */
    private final String pathToOutputDir;

    /**
     * Names of all programs to be analysed by this visitor.
     */
    private final Set<String> programs; // typesnames

    /**
     * Models to serialize during next serialization.
     */
    private Set<Model> modelsToSerialize;

    /**
     * Mapping model id number => model data.
     */
    private final Map<Integer, ModelData> id2modelData;

    /**
     * Mapping model => model id.
     */
    private Map<Model, Integer> model2id;

    /**
     * Number of models created by this analyser. Used to generate model id.
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
     * Model of the currently processed script or procedure definition.
     */
    private Model currentModel;

    /**
     * Mapping id => state of the current model.
     */
    private Map<Integer, State> states = new HashMap<>();

    /**
     * The state which will be used as starting edge for the next transition.
     */
    private State transitionStart;

    /**
     * The state which will be used as end point for the next transition.
     */
    private State transitionEnd;

    /**
     * List storing all the indexes of states which have to be connected to the
     * exit state.
     */
    private List<Integer> statesToExit = new LinkedList<>();

    /**
     * If a blocking if statement occurs this is used to prevent further
     * transitions to be added to the current model.
     */
    private boolean endAnalysis = false;

    /**
     * Location of the dot output files.
     */
    private final String dotOutputPath;

    /**
     * Creates a new instance of this visitor.
     *
     * @param pathToOutputDir Directory to hold the models.
     * @param programs        Names of all programs that will be processed.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public AUMVisitor(String pathToOutputDir, String dotOutputPath, Set<String> programs) {
        this.pathToOutputDir = pathToOutputDir;
        this.programs = programs;
        id2modelData = new HashMap<>();
        model2id = new HashMap<>();
        modelsToSerialize = new HashSet<>();
        modelsCreated = 0;
        currentModel = new Model();
        currentActorName = "";
        this.dotOutputPath = dotOutputPath;

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
                int i = 0;
                for (Model model : modelsToSerialize) {
                    i++;
                    if (dotOutputPath != null) {
                        saveToDotfile(i, model);
                    }
                    model.minimize();
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
        modelsToSerialize = new HashSet<>();
        model2id = new HashMap<>();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveToDotfile(int i, Model model) throws IOException {
        File dir = new File(dotOutputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (File entry : dir.listFiles()) {
            entry.delete();
        }
        File dotfile = new File(dotOutputPath + "aum-with" + i + ".dot");
        dotfile.createNewFile();
        model.saveToDotFile(dotfile);
    }

    /**
     * Called after analysis of a procedure definition is done.
     */
    public void endProcDefAnalysis(Model toAdd, String procDefId) {
        AUMExtractor.newProcDefAnalysed();
        endAnalysis(toAdd, procDefId, PROC_DEF);
    }

    /**
     * Called after analysis of a script is done.
     *
     * @param toAdd The model to be added to the models to serialise.
     */
    public void endScriptAnalysis(Model toAdd, String scriptId) {
        AUMExtractor.newScriptAnalysed();
        endAnalysis(toAdd, scriptId, SCRIPT);
    }

    /**
     * Called after analysis of both scripts and procedure definitions is done.
     *
     * @param toAdd    The model to be added to the models to serialise.
     * @param methodId The id of the script or procedure definition
     *                 analysis of which resulted in the model to add.
     */
    private void endAnalysis(Model toAdd, String methodId, String type) {
        for (Integer id : states.keySet()) {
            assert (id == states.get(id).getId());
        }
        modelsCreated++;
        modelsToSerialize.add(toAdd);
        model2id.put(toAdd, modelsCreated);
        id2modelData.put(modelsCreated, new ModelData("program: " +
                programName + " actor: " + currentActorName,
                type + ": " + methodId, ACTOR));
        clear();
    }

    /**
     * Clears the states of the visitor which are dependent on the current
     * script which is analysed.
     */
    private void clear() {
        endAnalysis = false;
        statesToExit = new LinkedList<>();
        states = new HashMap<>();
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
     * Called when analysis of a script results in an exception.
     */
    public void rollbackAnalysis() {
        currentActorName = "";
        if (currentModel != null) {
            modelsToSerialize.remove(currentModel);
            model2id.remove(currentModel);
        }
        clear();
    }

    /**
     * Updates the present state for the next transition to {@code to}.
     *
     * @param startOfTransition The state to which the next transition will be
     *                          added.
     */
    private void setTransitionStartTo(State startOfTransition) {
        assert !endAnalysis;
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
        assert !endAnalysis;
        AUMExtractor.newProjectPresent();
        programName = program.getIdent().getName();
        for (ActorDefinition def : program.getActorDefinitionList().getDefintions()) {
            currentActorName = def.getIdent().getName();
            for (ProcedureDefinition procDef : def.getProcedureDefinitionList().getList()) {
                procDef.accept(this);
            }
            for (Script script : def.getScripts().getScriptList()) {
                script.accept(this);
            }
            currentActorName = "";
        }
        serialiseModels();
    }

    /**
     * Creates an actor usage model of this procedure definition by iterating
     * every statement.
     *
     * @param procDef The procedure definition of which an actor usage model is
     *                to be created.
     */
    public void visit(ProcedureDefinition procDef) {
        assert !endAnalysis;
        AUMExtractor.newProcDefPresent();
        // add the procedure definition transition
        setTransitionStartTo(currentModel.getEntryState());
        states.put(transitionStart.getId(), transitionStart);
        addTransition(transitionStart, procDef.getUniqueName());
        // add the statements
        List<Stmt> listOfStmt = procDef.getStmtList().getStmts();
        addStmtList(listOfStmt);
        endProcDefAnalysis(new Model(currentModel), procDef.getIdent().getName());
    }

    /**
     * Creates an actor usage model of this script by iterating every statement.
     *
     * @param script The script of which an actor usage model is to be created.
     */
    @Override
    public void visit(Script script) {
        assert !endAnalysis;
        AUMExtractor.newScriptPresent();
        // add the event transition
        script.getEvent().accept(this);
        // add the statements
        List<Stmt> listOfStmt = script.getStmtList().getStmts();
        addStmtList(listOfStmt);
        endScriptAnalysis(new Model(currentModel), script.getId());
    }

    /**
     * Adds all the statements in the list to the current model.
     *
     * @param stmts List of statements to be added.
     */
    private void addStmtList(List<Stmt> stmts) {
        boolean lastIsForever = false;
        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            stmt.accept(this);
            if (i == stmts.size() - 1) {
                if (stmt instanceof RepeatForeverStmt) {
                    lastIsForever = true;
                }
            }
        }
        if (!lastIsForever) {
            EpsilonTransition returnTransition = EpsilonTransition.get();
            setTransitionStartTo(transitionEnd);
            transitionEnd = currentModel.getExitState();
            currentModel.addTransition(transitionStart, transitionEnd, returnTransition);
        }
        for (Integer integer : statesToExit) {
            transitionEnd = currentModel.getExitState();
            State state = states.get(integer);
            currentModel.addTransition(state, transitionEnd, EpsilonTransition.get());
        }
    }

    /**
     * Adds a transition for the event of the script and sets the present state
     * to the entry state of the model.
     *
     * @param event The event of the script currently analysed.
     */
    @Override
    public void visit(Event event) {
        assert !endAnalysis;
        setTransitionStartTo(currentModel.getEntryState());
        states.put(transitionStart.getId(), transitionStart);
        addTransition(transitionStart, event.getUniqueName());
    }

    /**
     * Adds a transition for the statement to the current model.
     *
     * @param stmt The statement causing the transition.
     */
    @Override
    public void visit(Stmt stmt) {
        if (!endAnalysis) {
            addTransitionContextAware(stmt.getUniqueName());
        }
    }

    /**
     * Adds a transition for the loop and adds transitions for its statement
     * list.
     *
     * @param loop The statement causing the transition.
     */
    @Override
    public void visit(RepeatForeverStmt loop) {
        if (!endAnalysis) {
            addLoopTransitions(loop.getUniqueName(), loop.getStmtList());
        }
    }

    /**
     * Adds a transition for the loop and adds transitions for its statement
     * list.
     *
     * @param loop The statement causing the transition.
     */
    @Override
    public void visit(RepeatTimesStmt loop) {
        if (!endAnalysis) {
            addLoopTransitions(loop.getUniqueName(), loop.getStmtList());
        }
    }

    /**
     * Adds a transition for the loop and adds transitions for its statement
     * list.
     *
     * @param loop The statement causing the transition.
     */
    @Override
    public void visit(UntilStmt loop) {
        if (!endAnalysis) {
            addLoopTransitions(loop.getUniqueName(), loop.getStmtList());
        }
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
        assert !endAnalysis;
        addTransitionContextAware(stmtName);
        int repeatStateIndex = transitionEnd.getId();
        List<Stmt> listOfStmt = stmtList.getStmts();
        boolean forever = false;
        boolean termination = false;
        for (int i = 0; i < listOfStmt.size(); i++) {
            Stmt stmt = listOfStmt.get(i);
            stmt.accept(this);
            if (i == listOfStmt.size() - 1) {
                if (stmt instanceof RepeatForeverStmt) {
                    forever = true;
                } else if (stmt instanceof StopAll || stmt instanceof StopThisScript) {
                    termination = true;
                }
            }
        }
        if (!forever && !termination) {
            setTransitionStartTo(transitionEnd);
            EpsilonTransition transition = EpsilonTransition.get();
            State repeatState = states.get(repeatStateIndex);
            currentModel.addTransition(transitionStart, repeatState, transition);
            transitionEnd = repeatState;
        } else if (termination) {
            statesToExit.add(transitionEnd.getId());
            transitionEnd = states.get(repeatStateIndex);
        } else {
            transitionEnd = states.get(repeatStateIndex);
        }
    }

    /**
     * Adds a transition for the if-statement itself, and for its true and false
     * branches and the statements in the branches.
     *
     * @param ifElseStmt The statement causing the transition.
     */
    @Override
    public void visit(IfElseStmt ifElseStmt) {
        if (!endAnalysis) {
            // add control stmt
            int afterIfStmtIndex = addTrueBranch(ifElseStmt.getUniqueName());
            // add true branch stmts
            List<Stmt> trueBranchStmts = ifElseStmt.getStmtList().getStmts();
            boolean foreverTrue = false;
            boolean terminationTrue = false;
            for (int i = 0; i < trueBranchStmts.size(); i++) {
                Stmt stmt = trueBranchStmts.get(i);
                stmt.accept(this);
                if (i == trueBranchStmts.size() - 1) {
                    if (stmt instanceof RepeatForeverStmt) {
                        foreverTrue = true;
                    } else if (stmt instanceof StopAll || stmt instanceof StopThisScript) {
                        terminationTrue = true;
                    }
                }
            }
            // save index of last state in true branch
            int endOfTrueBranchStateId = transitionEnd.getId();
            // add epsilon transition for the false branch
            transitionStart = states.get(afterIfStmtIndex);
            EpsilonTransition trans = EpsilonTransition.get();
            State follow = currentModel.getNewState();
            states.put(follow.getId(), follow);
            transitionEnd = follow;
            currentModel.addTransition(transitionStart, follow, trans);
            // add false branch stmts
            List<Stmt> listOfStmt = ifElseStmt.getElseStmts().getStmts();
            boolean foreverFalse = false;
            boolean terminationFalse = false;
            for (int i = 0; i < listOfStmt.size(); i++) {
                Stmt stmt = listOfStmt.get(i);
                stmt.accept(this);
                if (i == listOfStmt.size() - 1) {
                    if (stmt instanceof RepeatForeverStmt) {
                        foreverFalse = true;
                    } else if (stmt instanceof StopAll || stmt instanceof StopThisScript) {
                        terminationFalse = true;
                    }
                }
            }
            // the next state currently is the end of the false branch
            setTransitionStartTo(transitionEnd);
            if (terminationTrue && terminationFalse
                    || foreverTrue && foreverFalse
                    || terminationTrue && foreverFalse
                    || foreverTrue && terminationFalse) {
                // the if statement is blocking and the creation of this AUM
                // should end
                statesToExit.add(endOfTrueBranchStateId);
                statesToExit.add(transitionEnd.getId());
                endAnalysis = true;
            } else {
                joinBranches(endOfTrueBranchStateId, foreverTrue, foreverFalse,
                        terminationTrue, terminationFalse);
            }
        }
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
    private void joinBranches(int endOfTrueBranchStateId, boolean foreverTrue,
                              boolean foreverFalse, boolean terminationTrue,
                              boolean terminationFalse) {
        assert !endAnalysis;
        State join = null;
        // false transition
        if (terminationFalse) {
            statesToExit.add(transitionStart.getId());
        } else {
            if (!foreverFalse) {
                join = currentModel.getNewState();
                states.put(join.getId(), join);
                EpsilonTransition falseTransition = EpsilonTransition.get();
                currentModel.addTransition(transitionStart, join, falseTransition);
            }
            // else: there is a repeat forever stmt at the end of the false branch
            // so do not add any further transitions
        }
        // add epsilon transition after true branch
        if (terminationTrue) {
            statesToExit.add(endOfTrueBranchStateId);
        } else {
            if (!foreverTrue) {
                if (join == null) {
                    join = currentModel.getNewState();
                    states.put(join.getId(), join);
                }
                State endOfTrueBranch = states.get(endOfTrueBranchStateId);
                setTransitionStartTo(endOfTrueBranch);
                EpsilonTransition trueTransition = EpsilonTransition.get();
                currentModel.addTransition(transitionStart, join, trueTransition);
            }
            // else: there is a repeat forever stmt at the end of the true branch
            // so do not add any further transitions
        }
        // make sure the following transitions use the right transition end as
        // new transition start
        if (join != null) {
            transitionEnd = join;
        }
    }

    /**
     * Adds a transition for the if-statement itself, and for its true and false
     * branches and the statements in the true branch.
     *
     * @param ifThenStmt The statement causing the transition.
     */
    @Override
    public void visit(IfThenStmt ifThenStmt) {
        if (!endAnalysis) {
            // add control stmt
            int afterIfStmtIndex = addTrueBranch(ifThenStmt.getUniqueName());
            // add true branch stmts
            boolean foreverEndOfTrue = false;
            boolean terminationStmtEndOfTrue = false;
            List<Stmt> trueBranchStmts = ifThenStmt.getThenStmts().getStmts();
            for (int i = 0; i < trueBranchStmts.size(); i++) {
                Stmt stmt = trueBranchStmts.get(i);
                stmt.accept(this);
                if (i == trueBranchStmts.size() - 1) {
                    if (stmt instanceof RepeatForeverStmt) {
                        foreverEndOfTrue = true;
                    } else if (stmt instanceof StopAll || stmt instanceof StopThisScript) {
                        terminationStmtEndOfTrue = true;
                    }
                }
            }
            // save index of last state in true branch
            int endOfTrueBranchStateId = transitionEnd.getId();
            // add the false branch
            setTransitionStartTo(states.get(afterIfStmtIndex));
            State join = currentModel.getNewState();
            states.put(join.getId(), join);
            EpsilonTransition falseTransition = EpsilonTransition.get();
            currentModel.addTransition(transitionStart, join, falseTransition);
            // add the true branch epsilon transition if there is neither a
            // termination statement at the end nor a repeat forever loop
            if (terminationStmtEndOfTrue) {
                // add a transition to the exit state at the end of the AUM creation
                statesToExit.add(endOfTrueBranchStateId);
            } else {
                if (!foreverEndOfTrue) {
                    // add epsilon transition after true branch
                    State endOfTrueBranch = states.get(endOfTrueBranchStateId);
                    setTransitionStartTo(endOfTrueBranch);
                    EpsilonTransition trueTransition = EpsilonTransition.get();
                    currentModel.addTransition(transitionStart, join, trueTransition);
                }
                // else: there is a repeat forever stmt at the end of the true branch
                // so do not add any further transitions
            }
            // make sure the following transitions use the right transition end as
            // new transition start
            transitionEnd = join;
        }
    }

    /**
     * Adds the if statement and the epsilon transition for the true branch to
     * the current model.
     *
     * @param ifName Name of the if statement.
     * @return The id of the state after the if statement.
     */
    private int addTrueBranch(String ifName) {
        assert !endAnalysis;
        setTransitionStartTo(transitionEnd);
        addTransition(transitionStart, ifName);
        // save index of state after control stmt
        int afterIfStmtIndex = transitionEnd.getId();
        // add epsilon transition for true branch
        setTransitionStartTo(transitionEnd);
        EpsilonTransition transition = EpsilonTransition.get();
        State followUpState = currentModel.getFollowUpState(transitionStart, transition);
        states.put(followUpState.getId(), followUpState);
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
        assert !endAnalysis;
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
        assert !endAnalysis;
        MethodCall methodCall = new MethodCall(ACTOR, stmtName);
        InvokeMethodTransition transition = InvokeMethodTransition.get(methodCall, new ArrayList<>());
        State followUpState = currentModel.getFollowUpState(presentState, transition);
        states.put(followUpState.getId(), followUpState);
        transitionEnd = followUpState;
        currentModel.addTransition(presentState, followUpState, transition);
    }
}