package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.softevo.oumextractor.analysis.AnalyzeErrorException;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.analysis.Frame;
import org.softevo.oumextractor.analysis.Value;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for representing simple nodes in the control flow
 * graph.  A simple node is a node with only one "normal" successor node and
 * any number of possible "exceptional" successor nodes. These are instructions:
 * AALOAD, AASTORE, ACONST_NULL, ALOAD, ANEWARRAY, ARETURN, ARRAYLENGTH, ASTORE,
 * BALOAD, BASTORE, BIPUSH, CALOAD, CASTORE, CHECKCAST, D2F, D2I, D2L, DADD, DALOAD,
 * DASTORE, DCMPG, DCMPL, DCONST_0, DCONST_1, DDIV, DLOAD, DMUL, DNEG, DREM,
 * DRETURN, DSTORE, DSUB, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, F2D, F2I,
 * F2L, FADD, FALOAD, FASTORE, FCMPG, FCMPL, FCONST_0, FCONST_1, FCONST_2, FDIV,
 * FLOAD, FMUL, FNEG, FREM, FRETURN, FSTORE, FSUB, GETFIELD, GETSTATIC, GOTO, I2B,
 * I2C, I2D, I2F, I2L, I2S, IADD, IALOAD, IAND, IASTORE, ICONST_0, ICONST_1,
 * ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1, IDIV, IINC, ILOAD, IMUL, INEG,
 * INSTANCEOF, INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, IOR,
 * IREM, IRETURN, ISHL, ISHR, ISTORE, ISUB, IUSHR, IXOR, L2D, L2F, L2I, LADD,
 * LALOAD, LAND, LASTORE, LCMP, LCONST_0, LCONST_1, LDC, LDIV, LLOAD, LMUL, LNEG,
 * LOR, LREM, LRETURN, LSHL, LSHR, LSTORE, LSUB, LUSHR, LXOR, MULTIANEWARRAY, NEW,
 * NEWARRAY, POP, POP2, PUTFIELD, PUTSTATIC, RETURN, SALOAD, SASTORE, SIPUSH, SWAP.
 *
 * @author Andrzej Wasylkowski
 */
public final class SimpleNode extends BytecodeNode {

    /**
     * "Normal" successor of this node.
     */
    private Node successor;

    /**
     * Frame of the "normal" successor of this node.
     */
    private Frame successorFrame;

    /**
     * Creates new simple node encapsulating given instruction.
     *
     * @param insn       Instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public SimpleNode(AbstractInsnNode insn, int index, int lineNumber) {
        super(insn, index, lineNumber);
    }

    /**
     * Creates a deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public SimpleNode(SimpleNode other) {
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
        SimpleNode result = new SimpleNode(this);
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
        if (this.successor != null) {
            result.add(this.successor);
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

    /**
     * Clears all successor frames.
     */
    private void clearFrames() {
        clearExceptionsFrames();
        this.successorFrame = null;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.controlflow.Node#analyzeDataFlow(org.softevo.oumextractor.analysis.Analyzer)
     */
    @Override
    public void analyzeDataFlow(Analyzer analyzer)
            throws AnalyzeErrorException {
        clearFrames();

        Value value1, value2, value3, value4;

        // perform the operation
        switch (this.instruction.getOpcode()) {
            case Opcodes.AALOAD:
            case Opcodes.AASTORE:
            case Opcodes.ANEWARRAY:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.BALOAD:
            case Opcodes.BASTORE:
            case Opcodes.CALOAD:
            case Opcodes.CASTORE:
            case Opcodes.CHECKCAST:
            case Opcodes.D2F:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.DADD:
            case Opcodes.DALOAD:
            case Opcodes.DASTORE:
            case Opcodes.DCMPG:
            case Opcodes.DCMPL:
            case Opcodes.DDIV:
            case Opcodes.DMUL:
            case Opcodes.DNEG:
            case Opcodes.DREM:
            case Opcodes.DSUB:
            case Opcodes.F2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.FADD:
            case Opcodes.FALOAD:
            case Opcodes.FASTORE:
            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
            case Opcodes.FDIV:
            case Opcodes.FMUL:
            case Opcodes.FNEG:
            case Opcodes.FREM:
            case Opcodes.FSUB:
            case Opcodes.GETFIELD:
            case Opcodes.GETSTATIC:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2D:
            case Opcodes.I2F:
            case Opcodes.I2L:
            case Opcodes.I2S:
            case Opcodes.IADD:
            case Opcodes.IALOAD:
            case Opcodes.IASTORE:
            case Opcodes.IAND:
            case Opcodes.IDIV:
            case Opcodes.IINC:
            case Opcodes.IMUL:
            case Opcodes.INEG:
            case Opcodes.INSTANCEOF:
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.IOR:
            case Opcodes.IREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.ISUB:
            case Opcodes.IUSHR:
            case Opcodes.IXOR:
            case Opcodes.L2D:
            case Opcodes.L2F:
            case Opcodes.L2I:
            case Opcodes.LADD:
            case Opcodes.LALOAD:
            case Opcodes.LASTORE:
            case Opcodes.LAND:
            case Opcodes.LCMP:
            case Opcodes.LDC:
            case Opcodes.LDIV:
            case Opcodes.LMUL:
            case Opcodes.LNEG:
            case Opcodes.LOR:
            case Opcodes.LREM:
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LSUB:
            case Opcodes.LUSHR:
            case Opcodes.LXOR:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
            case Opcodes.MULTIANEWARRAY:
            case Opcodes.NEW:
            case Opcodes.NEWARRAY:
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
            case Opcodes.SALOAD:
            case Opcodes.SASTORE:
                analyzer.performOperation(this.instruction);
                break;

            case Opcodes.ACONST_NULL:
                value1 = analyzer.newNullValue();
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.ALOAD:
            case Opcodes.DLOAD:
            case Opcodes.FLOAD:
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
                VarInsnNode varInsnNode = (VarInsnNode) this.instruction;
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(
                        this.successorFrame.getLocalVariable(varInsnNode.var));
                break;

            case Opcodes.ARETURN:
            case Opcodes.DRETURN:
            case Opcodes.FRETURN:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
                Frame returnFrame = this.in.copy();
                value1 = returnFrame.popOperand();
                analyzer.returnFromMethod(returnFrame, value1);
                break;

            case Opcodes.ASTORE:
            case Opcodes.DSTORE:
            case Opcodes.FSTORE:
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
                varInsnNode = (VarInsnNode) this.instruction;
                this.successorFrame = this.in.copy();
                this.successorFrame.setLocalVariable(varInsnNode.var,
                        this.successorFrame.popOperand());
                break;

            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                IntInsnNode intInsnNode = (IntInsnNode) this.instruction;
                value1 = analyzer.newIntegerValue(intInsnNode.operand);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.DCONST_0:
                value1 = analyzer.newDoubleValue(0.0d);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.DCONST_1:
                value1 = analyzer.newDoubleValue(1.0d);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.DUP:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                this.successorFrame.pushOperand(value1);
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.DUP_X1:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                value2 = this.successorFrame.popOperand();
                this.successorFrame.pushOperand(value1);
                this.successorFrame.pushOperand(value2);
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.DUP_X2:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                value2 = this.successorFrame.popOperand();
                if (value2.getSize() == 1) {
                    value3 = this.successorFrame.popOperand();
                    this.successorFrame.pushOperand(value1);
                    this.successorFrame.pushOperand(value3);
                    this.successorFrame.pushOperand(value2);
                    this.successorFrame.pushOperand(value1);
                } else {
                    this.successorFrame.pushOperand(value1);
                    this.successorFrame.pushOperand(value2);
                    this.successorFrame.pushOperand(value1);
                }
                break;

            case Opcodes.DUP2:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                if (value1.getSize() == 1) {
                    value2 = this.successorFrame.popOperand();
                    this.successorFrame.pushOperand(value2);
                    this.successorFrame.pushOperand(value1);
                    this.successorFrame.pushOperand(value2);
                    this.successorFrame.pushOperand(value1);
                } else {
                    this.successorFrame.pushOperand(value1);
                    this.successorFrame.pushOperand(value1);
                }
                break;

            case Opcodes.DUP2_X1:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                value2 = this.successorFrame.popOperand();
                if (value1.getSize() == 1) {
                    value3 = this.successorFrame.popOperand();
                    this.successorFrame.pushOperand(value2);
                    this.successorFrame.pushOperand(value1);
                    this.successorFrame.pushOperand(value3);
                    this.successorFrame.pushOperand(value2);
                    this.successorFrame.pushOperand(value1);
                } else {
                    this.successorFrame.pushOperand(value1);
                    this.successorFrame.pushOperand(value2);
                    this.successorFrame.pushOperand(value1);
                }
                break;

            case Opcodes.DUP2_X2:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                value2 = this.successorFrame.popOperand();
                if (value1.getSize() == 1) {
                    value3 = this.successorFrame.popOperand();
                    if (value3.getSize() == 1) {
                        value4 = this.successorFrame.popOperand();
                        this.successorFrame.pushOperand(value2);
                        this.successorFrame.pushOperand(value1);
                        this.successorFrame.pushOperand(value4);
                        this.successorFrame.pushOperand(value3);
                        this.successorFrame.pushOperand(value2);
                        this.successorFrame.pushOperand(value1);
                    } else {
                        this.successorFrame.pushOperand(value2);
                        this.successorFrame.pushOperand(value1);
                        this.successorFrame.pushOperand(value3);
                        this.successorFrame.pushOperand(value2);
                        this.successorFrame.pushOperand(value1);
                    }
                } else {
                    if (value2.getSize() == 1) {
                        value3 = this.successorFrame.popOperand();
                        this.successorFrame.pushOperand(value1);
                        this.successorFrame.pushOperand(value3);
                        this.successorFrame.pushOperand(value2);
                        this.successorFrame.pushOperand(value1);
                    } else {
                        this.successorFrame.pushOperand(value1);
                        this.successorFrame.pushOperand(value2);
                        this.successorFrame.pushOperand(value1);
                    }
                }
                break;

            case Opcodes.FCONST_0:
                value1 = analyzer.newFloatValue(0.0f);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.FCONST_1:
                value1 = analyzer.newFloatValue(1.0f);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.FCONST_2:
                value1 = analyzer.newFloatValue(2.0f);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.GOTO:
            case Opcodes.NOP:
                this.successorFrame = this.in.copy();
                break;

            case Opcodes.ICONST_0:
                value1 = analyzer.newIntegerValue(0);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.ICONST_1:
                value1 = analyzer.newIntegerValue(1);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.ICONST_2:
                value1 = analyzer.newIntegerValue(2);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.ICONST_3:
                value1 = analyzer.newIntegerValue(3);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.ICONST_4:
                value1 = analyzer.newIntegerValue(4);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.ICONST_5:
                value1 = analyzer.newIntegerValue(5);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.ICONST_M1:
                value1 = analyzer.newIntegerValue(-1);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.LCONST_0:
                value1 = analyzer.newLongValue(0L);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.LCONST_1:
                value1 = analyzer.newLongValue(1L);
                this.successorFrame = this.in.copy();
                this.successorFrame.pushOperand(value1);
                break;

            case Opcodes.POP:
                this.successorFrame = this.in.copy();
                this.successorFrame.popOperand();
                break;

            case Opcodes.POP2:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                if (value1.getSize() == 1) {
                    this.successorFrame.popOperand();
                }
                break;

            case Opcodes.RETURN:
                returnFrame = this.in.copy();
                analyzer.returnFromMethod(returnFrame, null);
                break;

            case Opcodes.SWAP:
                this.successorFrame = this.in.copy();
                value1 = this.successorFrame.popOperand();
                value2 = this.successorFrame.popOperand();
                this.successorFrame.pushOperand(value1);
                this.successorFrame.pushOperand(value2);
                break;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " +
                        this.instruction.getOpcode());
        }

        // propagate the changes if analyzer did have to do something
        if (this.successorFrame != null) {
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
        propagateExceptions(analyzer);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(this.index).append(": ");
        result.append("SimpleNode: ");
        result.append(super.toString());
        return result.toString();
    }
}
