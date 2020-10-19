package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.softevo.oumextractor.analysis.AnalyzeErrorException;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to represent bytecode instructions, which are comparisons.
 * These are instructions: IF_ACMPEQ, IF_ACMPNE, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT,
 * IF_ICMPLE, IF_ICMPLT, IF_ICMPNE, IFEQ, IFGE, IFGT, IFLE, IFLT, IFNE, IFNONNULL,
 * IFNULL.
 *
 * @author Andrzej Wasylkowski
 */

/**
 * @author wasylkowski
 */
public final class ComparisonNode extends BytecodeNode {

    /**
     * "true" successor of this node.
     */
    private Node falseSuccessor;

    /**
     * "false" successor of this node.
     */
    private Node trueSuccessor;

    /**
     * Frame to be used as an entry frame to "true" successor.
     */
    private Frame trueFrame;

    /**
     * Frame to be used as an entry frame to "false" successor.
     */
    private Frame falseFrame;

    /**
     * Creates new comparison node, with both successor nodes uninitialized.
     *
     * @param insn       Bytecode instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public ComparisonNode(AbstractInsnNode insn, int index, int lineNumber) {
        super(insn, index, lineNumber);
        this.falseSuccessor = null;
        this.trueSuccessor = null;
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public ComparisonNode(ComparisonNode other) {
        // make a copy
        super(other);
        this.falseSuccessor = other.falseSuccessor;
        this.trueSuccessor = other.trueSuccessor;
        if (other.trueFrame != null) {
            this.trueFrame = other.trueFrame.copy();
        } else {
            this.trueFrame = null;
        }
        if (other.falseFrame != null) {
            this.falseFrame = other.falseFrame.copy();
        } else {
            other.falseFrame = null;
        }

        // update predecessors of this node successors
        if (this.falseSuccessor != null) {
            this.falseSuccessor.addPredecessor(this);
        }
        if (this.trueSuccessor != null) {
            this.trueSuccessor.addPredecessor(this);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getOut()
     */
    @Override
    public Frame getOut() {
        if (this.falseFrame != null) {
            return this.falseFrame;
        }
        return this.trueFrame;
    }

    /**
     * Sets a given node as a "false" successor of this node.
     *
     * @param successor Node to become a "false" successor of this node.
     */
    public void setFalseSuccessor(Node successor) {
        if (this.falseSuccessor != null) {
            this.falseSuccessor.removePredecessor(this);
        }
        this.falseSuccessor = successor;
        this.falseSuccessor.addPredecessor(this);
    }

    /**
     * Sets a given node as a "true" successor of this node.
     *
     * @param successor Node to become a "true" successor of this node.
     */
    public void setTrueSuccessor(Node successor) {
        if (this.trueSuccessor != null) {
            this.trueSuccessor.removePredecessor(this);
        }
        this.trueSuccessor = successor;
        this.trueSuccessor.addPredecessor(this);
    }

    /**
     * Returns frame of the "true" successor of this node creating it if needed.
     *
     * @return Frame of the "true" successor of this node.
     */
    public Frame getTrueSuccessorFrame() {
        if (this.trueFrame == null) {
            this.trueFrame = this.in.copy();
        }
        return this.trueFrame;
    }

    /**
     * Returns frame of the "false" successor of this node creating it if needed.
     *
     * @return Frame of the "false" successor of this node.
     */
    public Frame getFalseSuccessorFrame() {
        if (this.falseFrame == null) {
            this.falseFrame = this.in.copy();
        }
        return this.falseFrame;
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
        ComparisonNode result = new ComparisonNode(this);
        copies.put(this, result);
        result.setFalseSuccessor(this.falseSuccessor.copyNormalPath(copies));
        result.setTrueSuccessor(this.trueSuccessor.copyNormalPath(copies));

        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllSuccessors()
     */
    @Override
    public Set<Node> getAllSuccessors() {
        Set<Node> result = new HashSet<Node>();
        result.add(this.falseSuccessor);
        result.add(this.trueSuccessor);
        result.addAll(this.getExceptionalSuccessors());
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllDataflowSuccessors()
     */
    @Override
    public Set<Node> getAllDataflowSuccessors() {
        return getAllSuccessors();
    }

    public Set<Node> getAllDataflowPredecessors() {
        return this.getPredecessors();
    }

    /**
     * Clears all successor frames.
     */
    private void clearFrames() {
        clearExceptionsFrames();
        this.trueFrame = null;
        this.falseFrame = null;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#analyzeDataFlow(org.softevo.oumextractor.analysis.Analyzer)
     */
    @Override
    public void analyzeDataFlow(Analyzer analyzer)
            throws AnalyzeErrorException {
        clearFrames();

        // perform the operation
        switch (this.instruction.getOpcode()) {
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IFEQ:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFLT:
            case Opcodes.IFNE:
            case Opcodes.IFNONNULL:
            case Opcodes.IFNULL:
                analyzer.performOperation(this.instruction);
                break;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " +
                        this.instruction.getOpcode());
        }

        // propagate the changes
        if (this.trueFrame != null) {
            analyzer.markFrameAsAnalyzed(this.trueFrame, this.trueSuccessor);
            Frame mergedOut;
            if (this.trueSuccessor.getPredecessors().size() == 1) {
                mergedOut = this.trueFrame;
            } else {
                mergedOut = this.trueFrame.merge(this.trueSuccessor.in);
            }
            if (mergedOut != this.trueSuccessor.in) {
                this.trueSuccessor.setIn(mergedOut);
                analyzer.addNodeToAnalyze(this.trueSuccessor);
            }
        }
        if (this.falseFrame != null) {
            analyzer.markFrameAsAnalyzed(this.falseFrame, this.falseSuccessor);
            Frame mergedOut;
            if (this.falseSuccessor.getPredecessors().size() == 1) {
                mergedOut = this.falseFrame;
            } else {
                mergedOut = this.falseFrame.merge(this.falseSuccessor.in);
            }
            if (mergedOut != this.falseSuccessor.in) {
                this.falseSuccessor.setIn(mergedOut);
                analyzer.addNodeToAnalyze(this.falseSuccessor);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(this.index).append(": ");
        result.append("ComparisonNode: ");
        result.append(super.toString());
        return result.toString();
    }
}
