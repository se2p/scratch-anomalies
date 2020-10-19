package org.softevo.oumextractor.controlflow;

import org.softevo.oumextractor.analysis.AnalyzeErrorException;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to represent nodes in a control flow graph.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class Node {

    /**
     * Predecessors of this node.
     */
    private final Set<Node> predecessors;
    /**
     * "in" set for dataflow analyses.
     */
    private final Set<Integer> dataflowIn;
    /**
     * "out" set for dataflow analyses.
     */
    private final Set<Integer> dataflowOut;
    /**
     * "gen" set for dataflow analyses.
     */
    private final Set<Integer> dataflowGen;
    /**
     * "kill" set for dataflow analyses.
     */
    private final Set<Integer> dataflowKill;
    /**
     * Frame at the entry to this node.
     */
    protected Frame in;
    /**
     * Indices of live variables on the input to this node or <code>null</code>/
     */
    private Set<Integer> liveVariables;

    /**
     * Creates new abstract node with empty list of predecessors.
     */
    protected Node() {
        this.in = null;
        this.predecessors = new HashSet<Node>();
        this.dataflowIn = new HashSet<Integer>();
        this.dataflowOut = new HashSet<Integer>();
        this.dataflowGen = new HashSet<Integer>();
        this.dataflowKill = new HashSet<Integer>();
        this.liveVariables = null;
    }

    /**
     * Creates deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public Node(Node other) {
        if (other.in != null) {
            this.in = other.in.copy();
        } else {
            this.in = null;
        }
        this.predecessors = new HashSet<Node>();

        this.dataflowIn = new HashSet<Integer>();
        this.dataflowOut = new HashSet<Integer>();
        this.dataflowGen = new HashSet<Integer>();
        this.dataflowKill = new HashSet<Integer>();
    }

    /**
     * Returns all possible successors of this node.
     */
    public abstract Set<Node> getAllSuccessors();

    /**
     * Returns all possible dataflow successors of this node.
     */
    public abstract Set<Node> getAllDataflowSuccessors();

    /**
     * Clears all the dataflow sets.
     */
    void clearDataflowSets() {
        this.dataflowIn.clear();
        this.dataflowOut.clear();
        this.dataflowGen.clear();
        this.dataflowKill.clear();
    }

    /**
     * Adds given element to the 'gen' dataflow set.
     *
     * @param id Element id.
     */
    void addGenElement(int id) {
        this.dataflowGen.add(id);
    }

    /**
     * Adds given element to the 'kill' dataflow set.
     *
     * @param id Element id.
     */
    void addKillElement(int id) {
        this.dataflowKill.add(id);
    }

    /**
     * Returns the 'in' dataflow set.
     *
     * @return The 'in' dataflow set.
     */
    Set<Integer> getInSet() {
        return Collections.unmodifiableSet(this.dataflowIn);
    }

    /**
     * Changes the 'in' dataflow set so that it contains the same elements as
     * the set given.
     *
     * @param in Elements to be set as the new 'in' dataflow set.
     */
    void setInSet(Set<Integer> in) {
        this.dataflowIn.clear();
        this.dataflowIn.addAll(in);
    }

    /**
     * Returns the 'out' dataflow set.
     *
     * @return The 'out' dataflow set.
     */
    Set<Integer> getOutSet() {
        return Collections.unmodifiableSet(this.dataflowOut);
    }

    /**
     * Changes the 'out' dataflow set so that it contains the same elements as
     * the set given.
     *
     * @param in Elements to be set as the new 'out' dataflow set.
     */
    void setOutSet(Set<Integer> out) {
        this.dataflowOut.clear();
        this.dataflowOut.addAll(out);
    }

    /**
     * Returns the 'gen' dataflow set.
     *
     * @return The 'gen' dataflow set.
     */
    Set<Integer> getGenSet() {
        return Collections.unmodifiableSet(this.dataflowGen);
    }

    /**
     * Returns the 'gen' dataflow set.
     *
     * @return The 'kill' dataflow set.
     */
    Set<Integer> getKillSet() {
        return Collections.unmodifiableSet(this.dataflowKill);
    }

    /**
     * Returns set of indices of variables that are live at the entry to
     * this node.
     *
     * @return Set of indices of live variables.
     */
    public Set<Integer> getLiveVariables() {
        return Collections.unmodifiableSet(this.liveVariables);
    }

    /**
     * Sets the set of live variables indices at the input to this node to
     * the given set.
     *
     * @param live Set of live variables indices.
     */
    void setLiveVariables(Set<Integer> live) {
        this.liveVariables = new HashSet<Integer>(live);
    }

    /**
     * Gets the frame at the entry to this node.
     *
     * @return Frame at the entry to this node.
     */
    public Frame getIn() {
        return this.in;
    }

    /**
     * Sets the frame at the entry to this node to a given frame.
     *
     * @param frame Frame at the entry to this node.
     */
    public void setIn(Frame frame) {
        this.in = frame;
    }

    /**
     * Adds given node as predecessor of this node.
     *
     * @param predecessor Node to be added as predecessor of this node.
     */
    public void addPredecessor(Node predecessor) {
        if (predecessor == null) {
            throw new InternalError();
        }
        this.predecessors.add(predecessor);
    }

    /**
     * Removes given node from the list of predecessors of this node.
     *
     * @param predecessor Node to remove from the list of predecessors.
     */
    public void removePredecessor(Node predecessor) {
        if (predecessor == null) {
            throw new InternalError();
        }
        this.predecessors.remove(predecessor);
    }

    /**
     * Returns list of predecessors of this node.
     *
     * @return List of predecessors of this node.
     */
    public Set<Node> getPredecessors() {
        return Collections.unmodifiableSet(this.predecessors);
    }

    /**
     * Returns one of the "normal" output frames of this frame.
     *
     * @return One of the "normal" output frames of this frame or
     * <code>null</code> if there is no "normal" output frame.
     */
    public abstract Frame getOut();

    /**
     * Copies a path consisting of this node and its "normal" successors
     * (transitively).  Nodes that are keys of the map given are not copied, but
     * rather their existing copies are used.  This prevents infinite recursion
     * in case of loops.
     *
     * @param copies Existing copies to be used.
     * @return Copy of this node being an entry to the copied path.
     */
    public abstract Node copyNormalPath(Map<Node, Node> copies);

    /**
     * Performs one step of a data flow analysis.  The analysis is performed on
     * this node and the output frame is propagated to successor nodes, which are
     * in turn inserted to the analyzer's list of nodes to analyze if their input
     * has changed.
     *
     * @param analyzer Analyzer to use.
     * @throws AnalyzeErrorException if the node can't be analyzed
     */
    public abstract void analyzeDataFlow(Analyzer analyzer)
            throws AnalyzeErrorException;
}
