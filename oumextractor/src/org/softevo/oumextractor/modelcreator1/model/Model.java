package org.softevo.oumextractor.modelcreator1.model;

import org.softevo.jutil.Triple;
import org.softevo.jutil.graphs.Graph;
import org.softevo.oumextractor.controlflow.BytecodeNode;
import org.softevo.oumextractor.controlflow.Node;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is used to represent so-called object usage models.  All mappings
 * in this class that use states as first-level keys (i.e. not as keys in
 * structures not being fields of some instance of this class) may hold
 * incorrect data.  Data correctness is guaranteed only when using state's
 * representative as a key.
 *
 * @author Andrzej Wasylkowski
 */
public final class Model implements Serializable {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -6804663525245561751L;

    /**
     * Entry state of the model.
     */
    private State entryState;

    /**
     * Exit state of the model.
     */
    private State exitState;

    /**
     * Mapping fully qualified exception class name => abnormal exit state.
     */
    private transient Map<String, AbnormalExitState> abnormalExitStates;

    /**
     * Last used state id: for generating unique anonymous states.
     */
    private int lastUsedStateId;

    /**
     * Model's core structure and content.
     */
    private Graph<State, Transition> model;

    // Fields below are not inherent part of the model.  Therefore they should
    // not be serialized, etc.

    /**
     * Mapping bytecode node => corresponding model state.
     */
    private transient Map<Node, State> node2state;

    /**
     * Mapping starting state => (mapping transition => follow-up state).
     */
    private transient Map<State, Map<Transition, State>> state2followup;

    /**
     * Creates new, empty model, having only an anonymous entry state and no
     * transitions.
     */
    public Model() {
        this.model = new Graph<State, Transition>();
        this.lastUsedStateId = 0;
        this.abnormalExitStates = new HashMap<String, AbnormalExitState>();
        this.node2state = new HashMap<Node, State>();
        this.state2followup = new HashMap<State, Map<Transition, State>>();

        this.entryState = getNewState();
        this.exitState = null;
    }

    /**
     * Creates new model equal to given model.
     *
     * @param model Model to be copied.
     */
    public Model(Model model) {
        this.entryState = model.entryState;
        this.exitState = model.exitState;
        this.lastUsedStateId = model.lastUsedStateId;
        this.model = new Graph<State, Transition>(model.model);
        this.abnormalExitStates = new HashMap<String, AbnormalExitState>(
                model.abnormalExitStates);

        this.node2state = new HashMap<Node, State>(model.node2state);
        this.state2followup = new HashMap<State, Map<Transition, State>>();
        for (Entry<State, Map<Transition, State>> entry : model.state2followup.entrySet()) {
            this.state2followup.put(entry.getKey(),
                    new HashMap<Transition, State>(entry.getValue()));
        }
    }

    /**
     * Returns the graph underlying this object usage model. WARNING: the graph
     * returned is not a copy; any changes made to it will be reflected in the
     * model and can lead to data integrity loss.
     *
     * @return The graph underlying this object usage model.
     */
    public Graph<State, Transition> getUnderlyingGraph() {
        return this.model;
    }

    /**
     * Clears unnecessary fields of this model, so as to facilitate garbage
     * collection.
     */
    public void clear() {
        this.node2state.clear();
        for (Map<Transition, State> followup : this.state2followup.values()) {
            followup.clear();
        }
        this.state2followup.clear();
    }

    /**
     * Returns all transitions of this model.
     *
     * @return All transitions of this model.
     */
    public Set<Transition> getAllTransitions() {
        return this.model.getEdges();
    }

    /**
     * Adds new state to states of this model.
     *
     * @param s State to add to this model.
     */
    private void addState(State s) {
        this.model.addVertex(s);
    }

    /**
     * Returns new unique state.
     *
     * @return New unique state.
     */
    public State getNewState() {
        State result = new AnonymousState(getNewStateId());
        addState(result);
        return result;
    }

    /**
     * Returns new unique state with given id.
     *
     * @return New unique state with given id.
     */
    public State getNewState(int id) {
        State result = new AnonymousState(id);
        addState(result);
        return result;
    }

    /**
     * Returns entry state of this model.
     *
     * @return Entry state of this model.
     */
    public State getEntryState() {
        return this.entryState;
    }

    /**
     * Returns exit state of this model.
     *
     * @return Exit state of this model.
     */
    public State getExitState() {
        if (this.exitState == null) {
            this.exitState = getNewState();
        }
        return this.exitState;
    }

    /**
     * Returns abnormal exit state associated with given exception.
     *
     * @param excType Fully qualified name of an exception class.
     * @return Abnormal exit state associated with given exception.
     */
    public State getAbnormalExitState(String excType) {
        if (!this.abnormalExitStates.containsKey(excType)) {
            AbnormalExitState state = new AbnormalExitState(getNewStateId(),
                    excType);
            this.abnormalExitStates.put(excType, state);
            addState(state);
        }
        return this.abnormalExitStates.get(excType);
    }

    /**
     * Returns state associated with given bytecode node.
     *
     * @param node Node, whose state association is looked for.
     * @return State associated with given node.
     */
    public State getStateForNode(Node node) {
        if (!this.node2state.containsKey(node)) {
            if (node instanceof BytecodeNode) {
                BytecodeNode bn = (BytecodeNode) node;
                this.node2state.put(node, getNewState(-bn.getIndex()));
            } else {
                this.node2state.put(node, getNewState());
            }
        }
        return this.node2state.get(node);
    }

    /**
     * Returns state, that is to be used as a follow-up state when going
     * from any of given starting states using given transition.
     *
     * @param start      Set of starting states.
     * @param transition Transition to be used.
     * @return Follow-up state.
     */
    public State getFollowUpState(State start, Transition transition) {
        if (start == null) {
            throw new IllegalArgumentException();
        }

        // get all possible existing results
        if (!this.state2followup.containsKey(start)) {
            this.state2followup.put(start, new HashMap<Transition, State>());
        }
        Map<Transition, State> tr2followup = this.state2followup.get(start);
        if (!tr2followup.containsKey(transition)) {
            tr2followup.put(transition, getNewState());
        }
        return tr2followup.get(transition);
    }

    /**
     * Adds given transition between given states.
     *
     * @param from       Starting point of a transition.
     * @param to         Ending point of a transition.
     * @param transition Transition description.
     */
    public void addTransition(State from, State to, Transition transition) {
        this.model.addEdge(from, to, transition);
    }

    /**
     * Saves this model to a 'dot' file.
     *
     * @param file 'dot' file.
     */
    public void saveToDotFile(File file) throws FileNotFoundException {
        PrintStream ps = new PrintStream(file);
        ps.print(getDotRepresentation());
        ps.close();
    }

    /**
     * Returns representation of this model as a 'dot' script.
     *
     * @return 'dot' script representation of this model.
     */
    private String getDotRepresentation() {
        StringBuffer result = new StringBuffer();
        result.append("digraph {\n");
        result.append("page=\"9,11\";\n");
        result.append("size=\"20,20\";\n");

        result.append("node [fillcolor=\"#FFAB19\", shape=circle, width=.4, fixedsize=true];\n");
        result.append(getDotRepresentation(getEntryState())).append(" [style=\"filled\", label=\"\"];\n");
        for (State s : this.model.getVertices()) {
            result.append(getDotRepresentation(s)).append(" [style=\"filled\", label=\"\"];\n");
        }

        for (State from : this.model.getVertices()) {
            for (State to : this.model.getSuccessors(from)) {
                StringBuffer label = new StringBuffer();
                label.append("label=\" ");
                for (Transition transition : this.model.getEdges(from, to)) {
                    label.append(transition.getDotString()).append("\\n");
                }
                label.append("\"");

                result.append(getDotRepresentation(from)).append("->");
                result.append(getDotRepresentation(to));
                result.append("[");
                result.append("fontsize=30, ");
                result.append(label.toString());
                result.append("];\n");
            }
        }
        result.append("}\n");
        return result.toString();
    }

    /**
     * Returns "dot" representation of given state.
     *
     * @param state State.
     * @return "dot" representation of given state.
     */
    private String getDotRepresentation(State state) {
        if (state == this.entryState) {
            if (state == this.exitState) {
                return "\"ENTRY/EXIT\"";
            } else {
                return "\"ENTRY\"";
            }
        } else if (state == this.exitState) {
            return "\"EXIT\"";
        } else {
            return state.getDotString();
        }
    }

    /**
     * Compresses all epsilon-only paths.  This is done for all such two
     * states a and b, that there is an epsilon transition from a to b and
     * a has no other outgoing transitions (A).  Then both states are merged.
     * States are also merged when there is an epsilon transition between them
     * and the second state has no other ingoing transitions (B).  Also, all
     * epsilon self-loops are removed.  This is being done until there are
     * no more such states.
     */
    public void minimize() {
        boolean changed = true;
        while (changed) {
            changed = false;
            changed |= compressEpsilonTransitionsA();
            changed |= compressEpsilonTransitionsB();
            changed |= compressEpsilonLoops();
        }
    }

    /**
     * Merges in the model all such two states a and b that there is an epsilon
     * transition from a to b and a has no other outgoing transitions.
     *
     * @return <code>true</code> if any merge has been done;
     * <code>false</code> otherwise
     */
    private boolean compressEpsilonTransitionsA() {
        Set<State> statesOutOfConsideration = new HashSet<State>();

        boolean changed = false;
        for (State a : new HashSet<State>(this.model.getVertices())) {
            if (statesOutOfConsideration.contains(a)) {
                continue;
            }

            // make sure a is not an entry state
            if (a == this.entryState) {
                continue;
            }

            Set<State> aSuccessors = this.model.getSuccessors(a);
            if (aSuccessors.size() != 1) {
                continue;
            }
            State b = aSuccessors.iterator().next();

            // make sure a is not equal to b
            if (a == b) {
                continue;
            }

            // check if there is an epsilon transition from a to b
            // and a has no other outgoing transitions
            Set<Transition> abEdges = this.model.getEdges(a, b);
            if (abEdges.size() == 1 &&
                    abEdges.contains(EpsilonTransition.get())) {
                // merge the two states (remove a and leave b)
                this.model.mergeVertices(b, a);
                statesOutOfConsideration.add(a);
                statesOutOfConsideration.add(b);
                changed = true;
                continue;
            }
        }
        return changed;
    }

    /**
     * Merges in the model all such two states a and b that there is an epsilon
     * transition from a to b and b has no other ingoing transitions.
     *
     * @return <code>true</code> if any merge has been done;
     * <code>false</code> otherwise
     */
    private boolean compressEpsilonTransitionsB() {
        Set<State> statesOutOfConsideration = new HashSet<State>();

        boolean changed = false;
        for (State b : new HashSet<State>(this.model.getVertices())) {
            if (statesOutOfConsideration.contains(b)) {
                continue;
            }

            // make sure b is not an entry or exit state (normal or abnormal)
            if (b == this.entryState ||
                    b == this.exitState ||
                    this.abnormalExitStates.values().contains(b)) {
                continue;
            }

            Set<State> bPredecessors = this.model.getPredecessors(b);
            if (bPredecessors.size() != 1) {
                continue;
            }
            State a = bPredecessors.iterator().next();

            // make sure a is not equal to b
            if (a == b) {
                continue;
            }

            // check if there is an epsilon transition from a to b
            // and b has no other ingoing transitions
            Set<Transition> abEdges = this.model.getEdges(a, b);
            if (abEdges.size() == 1 &&
                    abEdges.contains(EpsilonTransition.get())) {
                // merge the two states (remove b and leave a)
                this.model.mergeVertices(a, b);
                statesOutOfConsideration.add(a);
                statesOutOfConsideration.add(b);
                changed = true;
                continue;
            }
        }
        return changed;
    }

    /**
     * Removes all epsilon self-loops.
     *
     * @return <code>true</code> if any remove has been done;
     * <code>false</code> otherwise
     */
    private boolean compressEpsilonLoops() {
        boolean changed = false;

        for (State s : this.model.getVertices()) {
            EpsilonTransition et = EpsilonTransition.get();
            if (this.model.getEdges(s, s).contains(et)) {
                this.model.removeEdge(s, s, et);
                changed = true;
            }
        }
        return changed;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();

        // list states
        Set<State> listedStates = new HashSet<State>();
        result.append("States:\n");
        result.append("==================================================\n");
        for (State from : this.model.getVertices()) {
            if (!listedStates.contains(from)) {
                result.append(getStringRepresentation(from)).append('\n');
                listedStates.add(from);
            }
        }

        // list transitions
        Set<Triple<State, State, Transition>> listedTransitions =
                new HashSet<Triple<State, State, Transition>>();
        result.append("Transitions:\n");
        result.append("==================================================\n");
        for (State from : this.model.getVertices()) {
            for (State to : this.model.getSuccessors(from)) {
                for (Transition transition : this.model.getEdges(from, to)) {
                    Triple<State, State, Transition> triple =
                            new Triple<State, State, Transition>(from, to, transition);
                    if (!listedTransitions.contains(triple)) {
                        result.append(getStringRepresentation(from));
                        result.append(" --");
                        result.append(transition.getVeryShortEventString());
                        result.append("--> ");
                        result.append(getStringRepresentation(to));
                        result.append('\n');
                        listedTransitions.add(triple);
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * Returns representation of given state as a string.
     *
     * @param state State.
     * @return Representation of given state as a string.
     */
    private String getStringRepresentation(State state) {
        if (state == this.entryState) {
            if (state == this.exitState) {
                return "\"ENTRY/EXIT\"";
            } else {
                return "\"ENTRY\"";
            }
        } else if (state == this.exitState) {
            return "\"EXIT\"";
        } else {
            return state.toString();
        }
    }

    /**
     * Returns new unique state id.
     *
     * @return New unique state id.
     */
    private int getNewStateId() {
        if (this.lastUsedStateId == Integer.MAX_VALUE) {
            throw new InternalError();
        }
        return ++this.lastUsedStateId;
    }

    /**
     * Serializes this model into given stream.
     *
     * @param out Stream to serialize this object to.
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        // write abnormalExitStates
        out.writeInt(this.abnormalExitStates.size());
        for (Map.Entry<String, AbnormalExitState> entry :
                this.abnormalExitStates.entrySet()) {
            out.writeObject(entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

    /**
     * Reads this model from given stream.
     *
     * @param in Stream to read this object from.
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void readObject(ObjectInputStream in)
            throws ClassNotFoundException, IOException {
        in.defaultReadObject();

        // read abnormalExitStates
        this.abnormalExitStates = new HashMap<String, AbnormalExitState>();
        int num = in.readInt();
        for (int i = 0; i < num; i++) {
            String key = (String) in.readObject();
            AbnormalExitState value = (AbnormalExitState) in.readObject();
            this.abnormalExitStates.put(key, value);
        }

        // set node2state
        this.node2state = new HashMap<Node, State>();

        // set state2followup
        this.state2followup = new HashMap<State, Map<Transition, State>>();
    }
}
