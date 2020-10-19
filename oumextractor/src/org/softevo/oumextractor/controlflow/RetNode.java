package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;
import org.softevo.oumextractor.analysis.ReturnAddressValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to represent nodes encapsulating bytecode "ret" instruction.
 *
 * @author Andrzej Wasylkowski
 */
public final class RetNode extends BytecodeNode {

    /**
     * All possible after-jsr successors of this node.
     */
    private Set<Node> afterJsrSuccessors;

    /**
     * Creates new instance of a node encapsulating bytecode "ret" instruction.
     *
     * @param insn       "ret" instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public RetNode(AbstractInsnNode insn, int index, int lineNumber) {
        super(insn, index, lineNumber);
        this.afterJsrSuccessors = null;
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public RetNode(RetNode other) {
        super(other);
        this.afterJsrSuccessors = null;
    }

    /**
     * Sets possible after-jsr successors of this node to given set.
     *
     * @param afterJsrSuccessors Possible after-jsr successors of this node.
     */
    public void setAfterJsrSuccessors(Set<Node> afterJsrSuccessors) {
        this.afterJsrSuccessors = afterJsrSuccessors;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getOut()
     */
    @Override
    public Frame getOut() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllSuccessors()
     */
    @Override
    public Set<Node> getAllSuccessors() {
        Set<Node> result = new HashSet<Node>();
        if (this.afterJsrSuccessors != null) {
            result.addAll(this.afterJsrSuccessors);
        }
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
     * @see org.softevo.oumextractor.controlflow.Node#copyNormalPath(java.util.Map)
     */
    @Override
    public Node copyNormalPath(Map<Node, Node> copies) {
        // if the copy exists, return it
        if (copies.containsKey(this)) {
            return copies.get(this);
        }

        // copy a path beginning at this node
        RetNode result = new RetNode(this);
        copies.put(this, result);
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#analyzeDataFlow(org.softevo.oumextractor.analysis.Analyzer)
     */
    @Override
    public void analyzeDataFlow(Analyzer analyzer) {
        Frame out = this.in.copy();

        Set<Node> retNodes;

        // perform the operation
        switch (this.instruction.getOpcode()) {
            case Opcodes.RET:
                VarInsnNode varInsnNode = (VarInsnNode) this.instruction;
                ReturnAddressValue returnValue =
                        (ReturnAddressValue) out.getLocalVariable(varInsnNode.var);
                retNodes = returnValue.getRetNodes();
                break;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " +
                        this.instruction.getOpcode());
        }

        // propagate the changes
        for (Node retNode : retNodes) {
            Frame newOut = out.copy();
            analyzer.markFrameAsAnalyzed(newOut, retNode);
            Frame mergedOut;
            if (retNode.getPredecessors().size() == 1) {
                mergedOut = newOut;
            } else {
                mergedOut = newOut.merge(retNode.in);
            }
            if (mergedOut != retNode.in) {
                retNode.setIn(mergedOut);
                analyzer.addNodeToAnalyze(retNode);
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
        result.append("RetNode: ");
        result.append(super.toString());
        return result.toString();
    }
}
