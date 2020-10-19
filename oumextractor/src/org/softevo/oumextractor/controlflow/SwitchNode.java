package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.softevo.oumextractor.analysis.AnalyzeErrorException;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to represent nodes that encapsulate bytecode "switch"
 * instructions.  These are instructions: LOOKUPSWITCH, TABLESWITCH.
 *
 * @author Andrzej Wasylkowski
 */
public final class SwitchNode extends BytecodeNode {

    /**
     * "key" successors of this node.
     */
    private final Map<Integer, Node> keySuccessors;
    /**
     * Entry frames for "key" successors of this node.
     */
    private final Map<Integer, Frame> keySuccessorFrames;
    /**
     * "default" successor of this node.
     */
    private Node defaultSuccessor;
    /**
     * Entry frame of the "default" successor of this node.
     */
    private Frame defaultSuccessorFrame;

    /**
     * Creates new instance of this node.
     *
     * @param insn       "switch" instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public SwitchNode(AbstractInsnNode insn, int index, int lineNumber) {
        super(insn, index, lineNumber);
        this.defaultSuccessor = null;
        this.defaultSuccessorFrame = null;
        this.keySuccessors = new HashMap<Integer, Node>();
        this.keySuccessorFrames = new HashMap<Integer, Frame>();
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public SwitchNode(SwitchNode other) {
        super(other);
        this.defaultSuccessor = other.defaultSuccessor;
        this.keySuccessors = new HashMap<Integer, Node>(other.keySuccessors);
        if (other.defaultSuccessorFrame != null) {
            this.defaultSuccessorFrame = other.defaultSuccessorFrame.copy();
        } else {
            this.defaultSuccessorFrame = null;
        }
        this.keySuccessorFrames = new HashMap<Integer, Frame>();
        for (Map.Entry<Integer, Frame> entry : other.keySuccessorFrames.entrySet()) {
            this.keySuccessorFrames.put(entry.getKey(),
                    entry.getValue().copy());
        }

        if (this.defaultSuccessor != null) {
            this.defaultSuccessor.addPredecessor(this);
        }
        for (Node node : this.keySuccessors.values()) {
            node.addPredecessor(this);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getOut()
     */
    @Override
    public Frame getOut() {
        for (Frame frame : this.keySuccessorFrames.values()) {
            if (frame != null) {
                return frame;
            }
        }
        return this.defaultSuccessorFrame;
    }

    /**
     * Sets a given node as a "default" successor of this node.
     *
     * @param successor Node to become a "default" successor of this node.
     */
    public void setDefaultSuccessor(Node successor) {
        if (this.defaultSuccessor != null) {
            this.defaultSuccessor.removePredecessor(this);
        }
        this.defaultSuccessor = successor;
        this.defaultSuccessor.addPredecessor(this);
    }

    /**
     * Adds a given node as a "key" successor of this node.
     *
     * @param key       Key, that triggers given successor.
     * @param successor Successor node.
     */
    public void addKeySuccessor(Integer key, Node successor) {
        if (this.keySuccessors.containsKey(key)) {
            this.keySuccessors.get(key).removePredecessor(this);
        }
        this.keySuccessors.put(key, successor);
        this.keySuccessors.get(key).addPredecessor(this);
    }

    /**
     * Returns frame of the "default" successor of this node creating it if needed.
     *
     * @return Frame of the "default" successor of this node.
     */
    public Frame getDefaultSuccessorFrame() {
        if (this.defaultSuccessorFrame == null) {
            this.defaultSuccessorFrame = this.in.copy();
        }
        return this.defaultSuccessorFrame;
    }

    /**
     * Returns frame of the "key" successor of this node creating it if needed.
     *
     * @param key Key that triggers the successor.
     * @return Frame of the "key" successor of this node.
     */
    public Frame getKeySuccessorFrame(Integer key) {
        if (!this.keySuccessorFrames.containsKey(key)) {
            this.keySuccessorFrames.put(key, this.in.copy());
        }
        return this.keySuccessorFrames.get(key);
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
        SwitchNode result = new SwitchNode(this);
        copies.put(this, result);
        result.setDefaultSuccessor(this.defaultSuccessor.copyNormalPath(copies));
        for (Integer key : this.keySuccessors.keySet()) {
            Node successorClone =
                    result.keySuccessors.get(key).copyNormalPath(copies);
            result.addKeySuccessor(key, successorClone);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#getAllSuccessors()
     */
    @Override
    public Set<Node> getAllSuccessors() {
        Set<Node> result = new HashSet<Node>();
        result.add(this.defaultSuccessor);
        result.addAll(this.keySuccessors.values());
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

    /**
     * Clears all successor frames.
     */
    private void clearFrames() {
        clearExceptionsFrames();
        this.defaultSuccessorFrame = null;
        this.keySuccessorFrames.clear();
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
            case Opcodes.LOOKUPSWITCH:
            case Opcodes.TABLESWITCH:
                analyzer.performOperation(this.instruction);
                break;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " +
                        this.instruction.getOpcode());
        }

        // propagate the changes
        if (this.defaultSuccessorFrame != null) {
            analyzer.markFrameAsAnalyzed(this.defaultSuccessorFrame,
                    this.defaultSuccessor);
            Frame mergedOut;
            if (this.defaultSuccessor.getPredecessors().size() == 1) {
                mergedOut = this.defaultSuccessorFrame;
            } else {
                mergedOut = this.defaultSuccessorFrame.merge(
                        this.defaultSuccessor.in);
            }
            if (mergedOut != this.defaultSuccessor.in) {
                this.defaultSuccessor.setIn(mergedOut);
                analyzer.addNodeToAnalyze(this.defaultSuccessor);
            }
        }
        for (Integer key : this.keySuccessorFrames.keySet()) {
            Node successor = this.keySuccessors.get(key);
            Frame newOut = this.keySuccessorFrames.get(key);
            analyzer.markFrameAsAnalyzed(newOut, successor);
            Frame mergedOut;
            if (successor.getPredecessors().size() == 1) {
                mergedOut = newOut;
            } else {
                mergedOut = newOut.merge(successor.in);
            }
            if (mergedOut != successor.in) {
                successor.setIn(mergedOut);
                analyzer.addNodeToAnalyze(successor);
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
        result.append("SwitchNode: ");
        result.append(super.toString());
        return result.toString();
    }
}
