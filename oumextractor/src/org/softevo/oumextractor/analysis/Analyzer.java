package org.softevo.oumextractor.analysis;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.softevo.oumextractor.JavaMethod;
import org.softevo.oumextractor.OUMExtractor;
import org.softevo.oumextractor.controlflow.Node;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * This class is used as a base class for data flow analyzers.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class Analyzer {

    /**
     * Queue of nodes to analyze.
     */
    private final Queue<Node> nodesToAnalyze;
    /**
     * Mapping node => return address value associated with this node.
     */
    private final Map<Node, ReturnAddressValue> nodesReturnAddresses;
    /**
     * Method being currently analyzed.
     */
    protected JavaMethod javaMethod;

    /**
     * Creates new instance of an analyzer.
     */
    public Analyzer() {
        this.nodesToAnalyze = new LinkedList<Node>();
        this.nodesReturnAddresses = new HashMap<Node, ReturnAddressValue>();
    }

    /**
     * Called before the start of the data flow analysis for a given method.
     *
     * @param method Method, which will be analyzed for a data flow.
     */
    public void startAnalysis(JavaMethod method) {
        this.javaMethod = method;
        this.nodesToAnalyze.clear();
        this.nodesReturnAddresses.clear();
    }

    /**
     * Called to start the data flow analysis.
     *
     * @param node Entry node to the method.
     */
    public final void analyze(Node entry) {
        OUMExtractor.newMethodPresent();
        this.nodesToAnalyze.offer(entry);
        while (!this.nodesToAnalyze.isEmpty()) {
            Node nextNode = this.nodesToAnalyze.remove();
            try {
                preAnalyzeNode(nextNode, nextNode.getIn());
                nextNode.analyzeDataFlow(this);
                postAnalyzeNode(nextNode);
            } catch (AnalyzeErrorException e) {
                rollbackAnalysis();
                System.err.println("Unable to analyze: " +
                        this.javaMethod.getFullName() + "; reason: " +
                        e.getMessage());
                e.printStackTrace(System.err);
                throw new InternalError();
            } catch (OutOfMemoryError e) {
                rollbackAnalysis();
                System.err.println("Unable to analyze: " +
                        this.javaMethod.getFullName() + "; reason: " +
                        e.getMessage());
                e.printStackTrace(System.err);
                throw new InternalError();
            }
        }
        OUMExtractor.newMethodAnalyzed();
    }

    /**
     * Called right after the data flow analysis for the analyzed method has
     * ended.
     */
    public void endAnalysis() {
    }

    /**
     * Called at the end of all analyses done using this analyzer.
     */
    public void shutdownAnalysis() {
    }

    /**
     * Called right before starting to analyze given node.
     *
     * @param node  Node which is about to get analyzed.
     * @param frame Input frame.
     */
    public void preAnalyzeNode(Node node, Frame frame) {
    }

    /**
     * Called right after finishing analyzing given node.
     *
     * @param node Node which has just been analyzed.
     */
    public void postAnalyzeNode(Node node) {
    }

    /**
     * Called by the code analyzing the actual instruction data flow to
     * inform the analyzer, that a given frame is the result of analysing
     * currently active node, that will be passed to a given node.
     *
     * @param frame     Frame, that is the result of the analysis.
     * @param successor Node, whose starting frame will be merged with given
     *                  frame in order to be analyzed.
     */
    public void markFrameAsAnalyzed(Frame frame, Node successor) {
    }

    /**
     * Called in order to roll the analysis back if an error occurred during
     * the analysis.
     */
    public void rollbackAnalysis() {
        this.nodesToAnalyze.clear();
        this.nodesReturnAddresses.clear();
    }

    /**
     * Called when an uninitialized frame is needed.  This is the case at the very
     * beginning of a data flow analysis for every frame of a method.
     *
     * @return Uninitialized frame.
     */
    public abstract Frame getInitFrame();

    /**
     * Called when an entry frame is needed.  This is the case at the very beginning
     * of a data flow analysis for the first frame of a method.  All parameters
     * should be initialized in this frame.
     *
     * @return Frame with only parameters of a method initialized.
     */
    public abstract Frame getEntryFrame();

    /**
     * Adds given node to the list of nodes that must be analyzed.  This method is
     * called whenever data flow analysis discovers that some input frame has
     * changed.
     *
     * @param node Node that needs to be analyzed.
     */
    public final void addNodeToAnalyze(Node node) {
        if (!this.nodesToAnalyze.contains(node)) {
            this.nodesToAnalyze.offer(node);
        }
    }

    /**
     * Called to indicate that the method execution could end with a given value and
     * given frame.
     *
     * @param frame Frame after executing the return instruction.
     * @param value Return value of an instruction or <code>null</code>.
     */
    public void returnFromMethod(Frame frame, Value value) {
    }

    /**
     * Called to indicate, that exception of given type could occur inside a method
     * being analyzed and no handler is found.
     *
     * @param excType Fully qualified exception type name that could occur.
     * @param out     Frame after executing instruction that could throw the
     *                exception.
     */
    public void unhandledException(String excType, Frame out) {
    }

    /**
     * Called when a specific operation is to be executed.  This can be: AALOAD,
     * AASTORE, ANEWARRAY, ARRAYLENGTH, ATHROW, BALOAD, BASTORE, CALOAD, CASTORE,
     * CHECKCAST, D2F, D2I, D2L, DADD, DALOAD, DASTORE, DCMPG, DCMPL, DDIV, DMUL,
     * DNEG, DREM, DSUB, F2D, F2I, F2L, FADD, FALOAD, FASTORE, FCMPG, FCMPL, FDIV,
     * FMUL, FNEG, FREM, FSUB, GETFIELD, GETSTATIC, I2B, I2C, I2D, I2F, I2L, I2S,
     * IADD, IALOAD, IAND, IASTORE, IDIV, IF_ACMPEQ, IF_ACMPNE, IF_ICMPEQ,
     * IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IFEQ, IFNE, IFLT,
     * IFGE, IFGT, IFLE, IMUL, IFNONNULL, IFNULL, IINC, INEG, INSTANCEOF,
     * INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, IOR, IREM, ISHL,
     * ISHR, ISUB, IUSHR, IXOR, L2D, L2F, L2I, LADD, LALOAD, LAND, LASTORE, LCMP,
     * LDC, LDIV, LMUL, LNEG, LOOKUPSWITCH, LOR, LREM, LSHL, LSHR, LSUB, LUSHR,
     * LXOR, MONITORENTER, MONITOREXIT, MULTIANEWARRAY, NEW, NEWARRAY,
     * PUTFIELD, PUTSTATIC, SALOAD, SASTORE, TABLESWITCH.
     *
     * @param node Instruction to be executed.
     * @throws AnalyzeErrorException if the node can't be analyzed
     */
    public abstract void performOperation(AbstractInsnNode node)
            throws AnalyzeErrorException;

    /**
     * Returns representation of a null value.
     *
     * @return Representation of a null value.
     */
    public abstract Value newNullValue();

    /**
     * Returns representation of an integer value given its initial value.
     *
     * @param value Initial value of the integer to represent.
     * @return Representation of the integer value.
     */
    public abstract Value newIntegerValue(int value);

    /**
     * Returns representation of a long value given its initial value.
     *
     * @param value Initial value of the long to represent.
     * @return Representation of the long value.
     */
    public abstract Value newLongValue(long value);

    /**
     * Returns representation of a double value given its initial value.
     *
     * @param value Initial value of the double to represent.
     * @return Representation of the double value.
     */
    public abstract Value newDoubleValue(double value);

    /**
     * Returns representation of a float value given its initial value.
     *
     * @param value Initial value of the float to represent.
     * @return Representation of the float value.
     */
    public abstract Value newFloatValue(float value);

    /**
     * Returns representation of an instance of a type (class or interface)
     * with given fully qualified name.
     *
     * @param className Fully qualified name of the type (class or interface).
     * @param exactType If <code>true</code>, the result will be a
     *                  representation of the exact type given; otherwise it
     *                  will represent the type given as well as all its
     *                  subtypes.
     * @return Representation of an object of given type.
     */
    public abstract Value newObjectValue(String typeName, boolean exactType);

    /**
     * Returns representation of a new return address value given the node
     * representing a next-to-JSR instruction.  This method must return the same
     * object for every call with the same parameter.
     *
     * @param node Node representing the next-to-JSR instruction for which the
     *             return address should be generated.
     * @return Representation of the return address value.
     */
    public final ReturnAddressValue newReturnAddressValue(Node retNode) {
        if (this.nodesReturnAddresses.containsKey(retNode)) {
            return this.nodesReturnAddresses.get(retNode);
        } else {
            return new ReturnAddressValue(retNode);
        }
    }

    /**
     * Splits given value into values, that constitute it, assigning to each of
     * those values a boolean, which indicates, whether the constituting value
     * is exactly of the type as represented or not.  This method is applicable
     * only to values that represent object values or sets of object values.
     *
     * @param v Value to be split.
     * @return Mapping from all constituting values to an indicator, whether
     * their type is exact.
     */
    public abstract Map<Value, Boolean> getExactValues(Value v);
}
