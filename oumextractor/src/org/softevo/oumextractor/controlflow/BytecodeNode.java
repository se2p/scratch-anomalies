package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.softevo.oumextractor.JavaClass;
import org.softevo.oumextractor.JavaClassPool;
import org.softevo.oumextractor.analysis.*;

import java.util.*;

/**
 * This class is used to represent nodes that encapsulate a bytecode instruction.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class BytecodeNode extends Node {

    /**
     * Index of the encapsulated instruction.
     */
    protected final int index;

    /**
     * Instruction encapsulated by this node.
     */
    protected final AbstractInsnNode instruction;

    /**
     * "Exceptional" successors of this node.
     */
    private final List<ExceptionHandler> handlers;

    /**
     * Frames after exceptions' throws: fully qualified name => frame.
     */
    private final Map<String, Frame> exceptionsFrames;

    /**
     * Line number of the instruction (or 0, if not known).
     */
    private final int lineNumber;

    /**
     * Creates new node encapsulating given instruction.
     *
     * @param insn       Instruction to encapsulate.
     * @param index      Index of the instruction.
     * @param lineNumber Line number of this node (if known) or zero.
     */
    public BytecodeNode(AbstractInsnNode insn, int index, int lineNumber) {
        this.index = index;
        this.instruction = insn;
        this.lineNumber = lineNumber;
        this.handlers = new ArrayList<ExceptionHandler>();
        this.exceptionsFrames = new HashMap<String, Frame>();
    }

    /**
     * Creates deep copy of given node.
     *
     * @param other Node to be copied.
     */
    public BytecodeNode(BytecodeNode other) {
        // make a copy
        super(other);
        this.index = other.index;
        this.instruction = other.instruction;
        this.lineNumber = other.lineNumber;
        this.handlers = new ArrayList<ExceptionHandler>(other.handlers);
        this.exceptionsFrames = new HashMap<String, Frame>();
        for (Map.Entry<String, Frame> entry : other.exceptionsFrames.entrySet()) {
            this.exceptionsFrames.put(entry.getKey(),
                    entry.getValue().copy());
        }

        // update predecessors of this node successors
        for (ExceptionHandler handler : this.handlers) {
            handler.getHandler().addPredecessor(this);
        }
    }

    /**
     * Returns index of the encapsulated instruction.
     *
     * @return Index of the encapsulated instruction.
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Returns line number of this node.
     *
     * @return Line number of this node (if known) or zero.
     */
    public int getLineNumber() {
        return this.lineNumber;
    }

    /**
     * Clears map of frames after exceptions' throws.
     */
    protected void clearExceptionsFrames() {
        this.exceptionsFrames.clear();
    }

    /**
     * Returns frame after given exception throw.  If the frame has already
     * been used by some code throwing the same exception, the exception is at
     * the top of the operand stack.  Otherwise <code>null</code> is at the top
     * of the operand stack.
     *
     * @param excType Fully qualified name of an exception class.
     * @return Frame after given exception throw.
     */
    public Frame getExceptionFrame(String excType) {
        if (!this.exceptionsFrames.containsKey(excType)) {
            Frame exceptionFrame = this.in.copy();
            exceptionFrame.pushOperand(null);
            this.exceptionsFrames.put(excType, exceptionFrame);
        }
        return this.exceptionsFrames.get(excType);
    }

    /**
     * Adds given exception object as a possible exception of given type.
     *
     * @param excType Fully qualified name of an exception class.
     * @param exc     Exception object to add.
     */
    public void addException(String excType, Value exc) {
        Frame exceptionFrame = getExceptionFrame(excType);
        Value previous = exceptionFrame.popOperand();
        if (previous == null) {
            exceptionFrame.pushOperand(exc);
        } else {
            exceptionFrame.pushOperand(exc.merge(previous,
                    new MergesCache()));
        }
    }

    /**
     * Adds new exception handler to this node.
     *
     * @param handler Exception handler to be added.
     */
    public void addExceptionHandler(ExceptionHandler handler) {
        this.handlers.add(handler);
        handler.getHandler().addPredecessor(this);
    }

    /**
     * Returns all nodes, that are possible "exceptional" successors of this node.
     *
     * @return All possible "exceptional" successors of this node.
     */
    protected Set<Node> getExceptionalSuccessors() {
        Set<Node> result = new HashSet<Node>();
        for (ExceptionHandler handler : this.handlers) {
            result.add(handler.getHandler());
        }
        return result;
    }

    /**
     * Returns first nodes of handlers of an exception of given type.
     *
     * @param excType   Fully qualified name of an exception to check.
     * @param typeExact Indicates, if the type of an exception to check is the
     *                  exact one, or just a base type of a possible type.
     * @return Set of first nodes of handlers of given exception.
     * @throws AnalyzeErrorException If an exception class can not be found.
     */
    private Set<Node> getHandlerNodes(String excType, boolean typeExact)
            throws AnalyzeErrorException {
        JavaClassPool pool = JavaClassPool.get();
        try {
            HashSet<Node> result = new HashSet<Node>();
            try {
                JavaClass excClass = (JavaClass) pool.getType(excType, false);
                for (ExceptionHandler handler : this.handlers) {
                    try {
                        // deal with catch-all handlers
                        if (handler.getExceptionType() == null) {
                            result.add(handler.getHandler());
                            break;
                        }

                        // deal with specific type handlers
                        JavaClass handlerClass = (JavaClass) pool.getType(
                                handler.getExceptionType(), false);
                        if (handlerClass.isAssignableFrom(excClass)) {
                            result.add(handler.getHandler());
                            break;
                        } else if (!typeExact &&
                                excClass.isAssignableFrom(handlerClass)) {
                            result.add(handler.getHandler());
                        }
                    } catch (ClassNotFoundException e) {
                        // ignore the error and try to proceed; this makes the
                        // analysis less precise, but also more robust
                    }
                }
            } catch (ClassNotFoundException e) {
                // ignore the error and try to proceed; this makes the
                // analysis less precise, but also more robust
                for (ExceptionHandler handler : this.handlers) {
                    // deal with catch-all handlers
                    if (handler.getExceptionType() == null) {
                        result.add(handler.getHandler());
                        break;
                    }
                }
            }

            return result;
        } catch (LinkageError e) {
            throw new AnalyzeErrorException(e);
        }
    }

    /**
     * Propagates changes to handlers' frames based on exceptions thrown.
     *
     * @param analyzer Analyzer to use.
     * @throws AnalyzeErrorException If an exception class can not be found.
     */
    protected void propagateExceptions(Analyzer analyzer)
            throws AnalyzeErrorException {
        for (String excType : this.exceptionsFrames.keySet()) {
            Frame frame = this.exceptionsFrames.get(excType);
            Value exc = frame.popOperand();
            frame.clearOperandsStack();
            Map<Value, Boolean> excExactValues = analyzer.getExactValues(exc);
            for (Value e : excExactValues.keySet()) {
                boolean eExact = excExactValues.get(e);
                Frame copy = frame.copy();
                copy.pushOperand(e);
                Set<Node> handlerNodes = getHandlerNodes(excType, eExact);
                if (handlerNodes.isEmpty()) {
                    analyzer.unhandledException(excType, copy);
                } else {
                    for (Node handlerNode : handlerNodes) {
                        Frame newIn = copy.copy();
                        analyzer.markFrameAsAnalyzed(newIn, handlerNode);
                        Frame out;
                        if (handlerNode.getPredecessors().size() == 1) {
                            out = newIn;
                        } else {
                            out = newIn.merge(handlerNode.in);
                        }
                        if (out != handlerNode.in) {
                            handlerNode.setIn(out);
                            analyzer.addNodeToAnalyze(handlerNode);
                        }
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        switch (this.instruction.getOpcode()) {
            case Opcodes.AALOAD:
                return "AALOAD";

            case Opcodes.AASTORE:
                return "AASTORE";

            case Opcodes.ACONST_NULL:
                return "ACONST_NULL";

            case Opcodes.ALOAD:
                return "ALOAD " + ((VarInsnNode) this.instruction).var;

            case Opcodes.ANEWARRAY:
                return "ANEWARRAY " + ((TypeInsnNode) this.instruction).desc;

            case Opcodes.ARETURN:
                return "ARETURN";

            case Opcodes.ARRAYLENGTH:
                return "ARRAYLENGTH";

            case Opcodes.ASTORE:
                return "ASTORE " + ((VarInsnNode) this.instruction).var;

            case Opcodes.ATHROW:
                return "ATHROW";

            case Opcodes.BALOAD:
                return "BALOAD";

            case Opcodes.BASTORE:
                return "BASTORE";

            case Opcodes.BIPUSH:
                return "BIPUSH " + ((IntInsnNode) this.instruction).operand;

            case Opcodes.CALOAD:
                return "CALOAD";

            case Opcodes.CASTORE:
                return "CASTORE";

            case Opcodes.CHECKCAST:
                return "CHECKCAST " + ((TypeInsnNode) this.instruction).desc;

            case Opcodes.D2F:
                return "D2F";

            case Opcodes.D2I:
                return "D2I";

            case Opcodes.D2L:
                return "D2L";

            case Opcodes.DADD:
                return "DADD";

            case Opcodes.DALOAD:
                return "DALOAD";

            case Opcodes.DASTORE:
                return "DASTORE";

            case Opcodes.DCMPG:
                return "DCMPG";

            case Opcodes.DCMPL:
                return "DCMPL";

            case Opcodes.DCONST_0:
                return "DCONST_0";

            case Opcodes.DCONST_1:
                return "DCONST_1";

            case Opcodes.DDIV:
                return "DDIV";

            case Opcodes.DLOAD:
                return "DLOAD " + ((VarInsnNode) this.instruction).var;

            case Opcodes.DMUL:
                return "DMUL";

            case Opcodes.DNEG:
                return "DNEG";

            case Opcodes.DREM:
                return "DREM";

            case Opcodes.DRETURN:
                return "DRETURN";

            case Opcodes.DSTORE:
                return "DSTORE " + ((VarInsnNode) this.instruction).var;

            case Opcodes.DSUB:
                return "DSUB";

            case Opcodes.DUP:
                return "DUP";

            case Opcodes.DUP_X1:
                return "DUP_X1";

            case Opcodes.DUP_X2:
                return "DUP_X2";

            case Opcodes.DUP2:
                return "DUP2";

            case Opcodes.DUP2_X1:
                return "DUP2_X1";

            case Opcodes.DUP2_X2:
                return "DUP2_X2";

            case Opcodes.F2D:
                return "F2D";

            case Opcodes.F2I:
                return "F2I";

            case Opcodes.F2L:
                return "F2L";

            case Opcodes.FADD:
                return "FADD";

            case Opcodes.FALOAD:
                return "FALOAD";

            case Opcodes.FASTORE:
                return "FASTORE";

            case Opcodes.FCMPG:
                return "FCMPG";

            case Opcodes.FCMPL:
                return "FCMPL";

            case Opcodes.FCONST_0:
                return "FCONST_0";

            case Opcodes.FCONST_1:
                return "FCONST_1";

            case Opcodes.FCONST_2:
                return "FCONST_2";

            case Opcodes.FDIV:
                return "FDIV";

            case Opcodes.FLOAD:
                return "FLOAD " + ((VarInsnNode) this.instruction).var;

            case Opcodes.FMUL:
                return "FMUL";

            case Opcodes.FNEG:
                return "FNEG";

            case Opcodes.FREM:
                return "FREM";

            case Opcodes.FRETURN:
                return "FRETURN";

            case Opcodes.FSTORE:
                return "FSTORE " + ((VarInsnNode) this.instruction).var;

            case Opcodes.FSUB:
                return "FSUB";

            case Opcodes.GETFIELD:
                FieldInsnNode fieldInsnNode = (FieldInsnNode) this.instruction;
                return "GETFIELD " + fieldInsnNode.owner.replace('/', '.') + "." +
                        fieldInsnNode.name;

            case Opcodes.GETSTATIC:
                fieldInsnNode = (FieldInsnNode) this.instruction;
                return "GETSTATIC " + fieldInsnNode.owner.replace('/', '.') + "." +
                        fieldInsnNode.name;

            case Opcodes.GOTO:
                return "GOTO";

            case Opcodes.I2B:
                return "I2B";

            case Opcodes.I2C:
                return "I2C";

            case Opcodes.I2D:
                return "I2D";

            case Opcodes.I2F:
                return "I2F";

            case Opcodes.I2L:
                return "I2L";

            case Opcodes.I2S:
                return "I2S";

            case Opcodes.IADD:
                return "IADD";

            case Opcodes.IALOAD:
                return "IALOAD";

            case Opcodes.IAND:
                return "IAND";

            case Opcodes.IASTORE:
                return "IASTORE";

            case Opcodes.ICONST_0:
                return "ICONST_0";

            case Opcodes.ICONST_1:
                return "ICONST_1";

            case Opcodes.ICONST_2:
                return "ICONST_2";

            case Opcodes.ICONST_3:
                return "ICONST_3";

            case Opcodes.ICONST_4:
                return "ICONST_4";

            case Opcodes.ICONST_5:
                return "ICONST_5";

            case Opcodes.ICONST_M1:
                return "ICONST_M1";

            case Opcodes.IDIV:
                return "IDIV";

            case Opcodes.IF_ACMPEQ:
                return "IF_ACMPEQ";

            case Opcodes.IF_ACMPNE:
                return "IF_ACMPNE";

            case Opcodes.IF_ICMPEQ:
                return "IF_ICMPEQ";

            case Opcodes.IF_ICMPGE:
                return "IF_ICMPGE";

            case Opcodes.IF_ICMPGT:
                return "IF_ICMPGT";

            case Opcodes.IF_ICMPLE:
                return "IF_ICMPLE";

            case Opcodes.IF_ICMPLT:
                return "IF_ICMPLT";

            case Opcodes.IF_ICMPNE:
                return "IF_ICMPNE";

            case Opcodes.IFEQ:
                return "IFEQ";

            case Opcodes.IFGE:
                return "IFGE";

            case Opcodes.IFGT:
                return "IFGT";

            case Opcodes.IFLE:
                return "IFLE";

            case Opcodes.IFLT:
                return "IFLT";

            case Opcodes.IFNE:
                return "IFNE";

            case Opcodes.IFNONNULL:
                return "IFNONNULL";

            case Opcodes.IFNULL:
                return "IFNULL";

            case Opcodes.IINC:
                IincInsnNode iincInsnNode = (IincInsnNode) this.instruction;
                return "IINC " + iincInsnNode.var + " by " + iincInsnNode.incr;

            case Opcodes.ILOAD:
                return "ILOAD " + ((VarInsnNode) this.instruction).var;

            case Opcodes.IMUL:
                return "IMUL";

            case Opcodes.INEG:
                return "INEG";

            case Opcodes.INSTANCEOF:
                return "INSTANCEOF " + ((TypeInsnNode) this.instruction).desc;

            case Opcodes.INVOKEINTERFACE:
                MethodInsnNode methodInsnNode = (MethodInsnNode) this.instruction;
                return "INVOKEINTERFACE " + methodInsnNode.owner.replace('/', '.') +
                        "." + methodInsnNode.name + methodInsnNode.desc;

            case Opcodes.INVOKESPECIAL:
                methodInsnNode = (MethodInsnNode) this.instruction;
                return "INVOKESPECIAL " + methodInsnNode.owner.replace('/', '.') +
                        "." + methodInsnNode.name + methodInsnNode.desc;

            case Opcodes.INVOKESTATIC:
                methodInsnNode = (MethodInsnNode) this.instruction;
                return "INVOKESTATIC " + methodInsnNode.owner.replace('/', '.') +
                        "." + methodInsnNode.name + methodInsnNode.desc;

            case Opcodes.INVOKEVIRTUAL:
                methodInsnNode = (MethodInsnNode) this.instruction;
                return "INVOKEVIRTUAL " + methodInsnNode.owner.replace('/', '.') +
                        "." + methodInsnNode.name + methodInsnNode.desc;

            case Opcodes.IOR:
                return "IOR";

            case Opcodes.IREM:
                return "IREM";

            case Opcodes.IRETURN:
                return "IRETURN";

            case Opcodes.ISHL:
                return "ISHL";

            case Opcodes.ISHR:
                return "ISHR";

            case Opcodes.ISTORE:
                return "ISTORE " + ((VarInsnNode) this.instruction).var;

            case Opcodes.ISUB:
                return "ISUB";

            case Opcodes.IUSHR:
                return "IUSHR";

            case Opcodes.IXOR:
                return "IXOR";

            case Opcodes.JSR:
                return "JSR";

            case Opcodes.L2D:
                return "L2D";

            case Opcodes.L2F:
                return "L2F";

            case Opcodes.L2I:
                return "L2I";

            case Opcodes.LADD:
                return "LADD";

            case Opcodes.LALOAD:
                return "LALOAD";

            case Opcodes.LAND:
                return "LAND";

            case Opcodes.LASTORE:
                return "LASTORE";

            case Opcodes.LCMP:
                return "LCMP";

            case Opcodes.LCONST_0:
                return "LCONST_0";

            case Opcodes.LCONST_1:
                return "LCONST_1";

            case Opcodes.LDC:
                return "LDC " + ((LdcInsnNode) this.instruction).cst.toString();

            case Opcodes.LDIV:
                return "LDIV";

            case Opcodes.LLOAD:
                return "LLOAD " + ((VarInsnNode) this.instruction).var;

            case Opcodes.LMUL:
                return "LMUL";

            case Opcodes.LNEG:
                return "LNEG";

            case Opcodes.LOOKUPSWITCH:
                return "LOOKUPSWITCH";

            case Opcodes.LOR:
                return "LOR";

            case Opcodes.LREM:
                return "LREM";

            case Opcodes.LRETURN:
                return "LRETURN";

            case Opcodes.LSHL:
                return "LSHL";

            case Opcodes.LSHR:
                return "LSHR";

            case Opcodes.LSTORE:
                return "LSTORE " + ((VarInsnNode) this.instruction).var;

            case Opcodes.LSUB:
                return "LSUB";

            case Opcodes.LUSHR:
                return "LUSHR";

            case Opcodes.LXOR:
                return "LXOR";

            case Opcodes.MONITORENTER:
                return "MONITORENTER";

            case Opcodes.MONITOREXIT:
                return "MONITOREXIT";

            case Opcodes.MULTIANEWARRAY:
                MultiANewArrayInsnNode multiANewArrayInsnNode =
                        (MultiANewArrayInsnNode) this.instruction;
                return "MULTIANEWARRAY " + multiANewArrayInsnNode.desc + " dim " +
                        multiANewArrayInsnNode.dims;

            case Opcodes.NEW:
                return "NEW " + ((TypeInsnNode) this.instruction).desc;

            case Opcodes.NEWARRAY:
                IntInsnNode intInsnNode = (IntInsnNode) this.instruction;
                switch (intInsnNode.operand) {
                    case 4:
                        return "NEWARRAY T_BOOLEAN";

                    case 5:
                        return "NEWARRAY T_CHAR";

                    case 6:
                        return "NEWARRAY T_FLOAT";

                    case 7:
                        return "NEWARRAY T_DOUBLE";

                    case 8:
                        return "NEWARRAY T_BYTE";

                    case 9:
                        return "NEWARRAY T_SHORT";

                    case 10:
                        return "NEWARRAY T_INT";

                    case 11:
                        return "NEWARRAY T_LONG";

                    default:
                        throw new InternalError("Unknown NEWARRAY operand: " +
                                intInsnNode.operand);
                }

            case Opcodes.NOP:
                return "NOP";

            case Opcodes.POP:
                return "POP";

            case Opcodes.POP2:
                return "POP2";

            case Opcodes.PUTFIELD:
                fieldInsnNode = (FieldInsnNode) this.instruction;
                return "PUTFIELD " + fieldInsnNode.owner.replace('/', '.') + "." +
                        fieldInsnNode.name;

            case Opcodes.PUTSTATIC:
                fieldInsnNode = (FieldInsnNode) this.instruction;
                return "PUTSTATIC " + fieldInsnNode.owner.replace('/', '.') + "." +
                        fieldInsnNode.name;

            case Opcodes.RET:
                return "RET " + ((VarInsnNode) this.instruction).var;

            case Opcodes.RETURN:
                return "RETURN";

            case Opcodes.SALOAD:
                return "SALOAD";

            case Opcodes.SASTORE:
                return "SASTORE";

            case Opcodes.SIPUSH:
                return "SIPUSH " + ((IntInsnNode) this.instruction).operand;

            case Opcodes.SWAP:
                return "SWAP";

            case Opcodes.TABLESWITCH:
                return "TABLESWITCH";

            default:
                throw new InternalError("Unknown instruction opcode: " +
                        this.instruction.getOpcode());
        }
    }
}
