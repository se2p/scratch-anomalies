package org.softevo.oumextractor.controlflow;

import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for representing an entry node in a control flow graph.
 *
 * @author Andrzej Wasylkowski
 */
public final class EntryNode extends Node {

    /**
     * Successor of this node.
     */
    private Node successor;

    /**
     * Creates new entry node.
     */
    public EntryNode() {
        this.successor = null;
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public EntryNode(EntryNode other) {
        super(other);
        this.successor = other.successor;

        this.successor.addPredecessor(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getOut()
     */
    @Override
    public Frame getOut() {
        return this.in;
    }

    /**
     * Sets new successor of this node.
     *
     * @param node New successor node.
     */
    public void setSuccessor(Node node) {
        if (this.successor != null) {
            this.successor.removePredecessor(this);
        }
        this.successor = node;
        this.successor.addPredecessor(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#copyNormalPath(java.util.Map)
     */
    @Override
    public Node copyNormalPath(Map<Node, Node> copies) {
        // if the copy exists, return it
        if (copies.containsKey(this)) {
            return copies.get(this);
        }

        // copy a path beginning at this node
        EntryNode result = new EntryNode(this);
        copies.put(this, result);
        result.setSuccessor(this.successor.copyNormalPath(copies));
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllSuccessors()
     */
    @Override
    public Set<Node> getAllSuccessors() {
        Set<Node> result = new HashSet<Node>();
        result.add(this.successor);
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllDataflowSuccessors()
     */
    @Override
    public Set<Node> getAllDataflowSuccessors() {
        return getAllSuccessors();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#analyzeDataFlow(org.softevo.oumextractor.analysis.Analyzer)
     */
    @Override
    public void analyzeDataFlow(Analyzer analyzer) {
        Frame out = this.in.copy();
        analyzer.markFrameAsAnalyzed(out, this.successor);
        Frame mergedOut;
        if (this.successor.getPredecessors().size() == 1) {
            mergedOut = out;
        } else {
            mergedOut = out.merge(this.successor.in);
        }
        if (mergedOut != this.successor.in) {
            this.successor.setIn(mergedOut);
            analyzer.addNodeToAnalyze(this.successor);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EntryNode";
    }
}
