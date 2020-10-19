package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;
import org.softevo.oumextractor.analysis.ReturnAddressValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for representing "jsr" bytecode instruction.
 *
 * @author Andrzej Wasylkowski
 */
public final class JsrNode extends BytecodeNode {

    /**
     * "jsr" successor of this node.
     */
    private Node jsrSuccessor;

    /**
     * "ret" successor of this node.
     */
    private Node retSuccessor;

    /**
     * Creates new instance of the node encapsulating "jsr" bytecode instruction.
     *
     * @param insn       "jsr" bytecode instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public JsrNode(AbstractInsnNode insn, int index, int lineNumber) {
        super(insn, index, lineNumber);
        this.jsrSuccessor = null;
        this.retSuccessor = null;
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public JsrNode(JsrNode other) {
        super(other);
        this.jsrSuccessor = other.jsrSuccessor;
        this.retSuccessor = other.retSuccessor;

        if (this.jsrSuccessor != null) {
            this.jsrSuccessor.addPredecessor(this);
        }
        if (this.retSuccessor != null) {
            this.retSuccessor.addPredecessor(this);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getOut()
     */
    @Override
    public Frame getOut() {
        return null;
    }

    /**
     * Returns "jsr" successor of this node.
     *
     * @return "jsr" successor of this node.
     */
    public Node getJsrSuccessor() {
        return this.jsrSuccessor;
    }

    /**
     * Sets a given nodes as a "jsr" successor of this node.
     *
     * @param successor Node to become a "jsr" successor of this node.
     */
    public void setJsrSuccessor(Node successor) {
        if (this.jsrSuccessor != null) {
            this.jsrSuccessor.removePredecessor(this);
        }
        this.jsrSuccessor = successor;
        this.jsrSuccessor.addPredecessor(this);
    }

    /**
     * Returns "ret" successor of this node.
     *
     * @return "ret" successor of this node.
     */
    public Node getRetSuccessor() {
        return this.retSuccessor;
    }

    /**
     * Sets a given nodes as a "ret" successor of this node.
     *
     * @param successor Node to become a "ret" successor of this node.
     */
    public void setRetSuccessor(Node successor) {
        if (this.retSuccessor != null) {
            this.retSuccessor.removePredecessor(this);
        }
        this.retSuccessor = successor;
        this.retSuccessor.addPredecessor(this);
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
        JsrNode result = new JsrNode(this);
        copies.put(this, result);
        result.setJsrSuccessor(this.jsrSuccessor.copyNormalPath(copies));
        result.setRetSuccessor(this.retSuccessor.copyNormalPath(copies));
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllSuccessors()
     */
    @Override
    public Set<Node> getAllSuccessors() {
        Set<Node> result = new HashSet<Node>();
        result.add(this.jsrSuccessor);
        result.add(this.retSuccessor);
        result.addAll(this.getExceptionalSuccessors());
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllDataflowSuccessors()
     */
    @Override
    public Set<Node> getAllDataflowSuccessors() {
        Set<Node> result = new HashSet<Node>();
        result.add(this.jsrSuccessor);
        result.addAll(this.getExceptionalSuccessors());
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#analyzeDataFlow(org.softevo.oumextractor.analysis.Analyzer)
     */
    @Override
    public void analyzeDataFlow(Analyzer analyzer) {
        Frame out = this.in.copy();

        // perform the operation
        switch (this.instruction.getOpcode()) {
            case Opcodes.JSR:
                ReturnAddressValue value =
                        analyzer.newReturnAddressValue(this.retSuccessor);
                out.pushOperand(value);
                break;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " +
                        this.instruction.getOpcode());
        }

        // propagate the changes
        analyzer.markFrameAsAnalyzed(out, this.jsrSuccessor);
        Frame mergedOut;
        if (this.jsrSuccessor.getPredecessors().size() == 1) {
            mergedOut = out;
        } else {
            mergedOut = out.merge(this.jsrSuccessor.in);
        }
        if (mergedOut != this.jsrSuccessor.in) {
            this.jsrSuccessor.setIn(mergedOut);
            analyzer.addNodeToAnalyze(this.jsrSuccessor);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(this.index).append(": ");
        result.append("JsrNode: ");
        result.append(super.toString());
        return result.toString();
    }
}
