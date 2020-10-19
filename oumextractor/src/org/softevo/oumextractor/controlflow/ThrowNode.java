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
 * This class is used to represent nodes that encapsulate "athrow" instruction.
 *
 * @author Andrzej Wasylkowski
 */
public final class ThrowNode extends BytecodeNode {

    /**
     * Creates new node that encapsulates the "athrow" instruction.
     *
     * @param insn       "athrow" instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public ThrowNode(AbstractInsnNode insn, int index, int lineNumber) {
        super(insn, index, lineNumber);
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public ThrowNode(ThrowNode other) {
        super(other);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getOut()
     */
    @Override
    public Frame getOut() {
        return null;
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
        ThrowNode result = new ThrowNode(this);
        copies.put(this, result);
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllSuccessors()
     */
    @Override
    public Set<Node> getAllSuccessors() {
        Set<Node> result = new HashSet<Node>();
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

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#analyzeDataFlow(org.softevo.oumextractor.analysis.Analyzer)
     */
    @Override
    public void analyzeDataFlow(Analyzer analyzer)
            throws AnalyzeErrorException {
        clearExceptionsFrames();

        // perform the operation
        switch (this.instruction.getOpcode()) {
            case Opcodes.ATHROW:
                analyzer.performOperation(this.instruction);
                break;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " +
                        this.instruction.getOpcode());
        }

        propagateExceptions(analyzer);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(this.index).append(": ");
        result.append("ThrowNode: ");
        result.append(super.toString());
        return result.toString();
    }
}
