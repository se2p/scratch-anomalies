package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.softevo.oumextractor.analysis.AnalyzeErrorException;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for representing empty nodes in the control flow
 * graph. These are nodes that are do not correspond to bytecode instructions,
 * but have an organizational meaning only that is irrevelant for the purposes
 * of the analysis. All such nodes have only one "normal" successor node and no
 * "exceptional" successor nodes.
 *
 * @author Andrzej Wasylkowski
 */
public final class EmptyNode extends BytecodeNode {

    /**
     * "Normal" successor of this node.
     */
    private Node successor;

    /**
     * Frame of the "normal" successor of this node.
     */
    private Frame successorFrame;

    /**
     * Creates a new empty node encapsulating given instruction.
     *
     * @param insn       Instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public EmptyNode(AbstractInsnNode insn, int index, int lineNumber) {
        super(insn, index, lineNumber);
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public EmptyNode(EmptyNode other) {
        super(other);
        this.successor = other.successor;
        if (other.successorFrame != null) {
            this.successorFrame = other.successorFrame.copy();
        } else {
            this.successorFrame = null;
        }

        if (this.successor != null) {
            this.successor.addPredecessor(this);
        }
    }

    /**
     * Sets the "normal" successor of this node to given node.
     *
     * @param node New "normal" successor of this node.
     */
    public void setSuccessor(Node node) {
        if (this.successor != null) {
            this.successor.removePredecessor(this);
        }
        this.successor = node;
        this.successor.addPredecessor(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getOut()
     */
    @Override
    public Frame getOut() {
        return this.successorFrame;
    }

    /**
     * Returns frame of the "normal" successor of this node creating it if needed.
     *
     * @return Frame of the "normal" successor of this node.
     */
    public Frame getSuccessorFrame() {
        if (this.successorFrame == null) {
            this.successorFrame = this.in.copy();
        }
        return this.successorFrame;
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
        EmptyNode result = new EmptyNode(this);
        copies.put(this, result);
        if (this.successor != null) {
            result.setSuccessor(this.successor.copyNormalPath(copies));
        }
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

    /**
     * Clears all successor frames.
     */
    private void clearFrames() {
        this.successorFrame = null;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#analyzeDataFlow(org.softevo.oumextractor.analysis.Analyzer)
     */
    @Override
    public void analyzeDataFlow(Analyzer analyzer)
            throws AnalyzeErrorException {
        clearFrames();
        this.successorFrame = this.in.copy();
        analyzer.markFrameAsAnalyzed(this.successorFrame, this.successor);
        Frame mergedOut;
        if (this.successor.getPredecessors().size() == 1) {
            mergedOut = this.successorFrame;
        } else {
            mergedOut = this.successorFrame.merge(this.successor.in);
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
        StringBuffer result = new StringBuffer();
        result.append(this.index).append(": ");
        result.append("EmptyNode: " + this.instruction.toString());
        return result.toString();
    }
}
