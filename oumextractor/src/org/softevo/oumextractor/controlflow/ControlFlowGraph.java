package org.softevo.oumextractor.controlflow;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.softevo.oumextractor.JavaMethod;

import java.util.*;

/**
 * This class is responsible for representing a control flow graph.
 *
 * @author Andrzej Wasylkowski
 */
public final class ControlFlowGraph {

    /**
     * Method, whose control flow graph is being represented.
     */
    private final JavaMethod method;

    /**
     * Entry node of the control flow graph.
     */
    private final EntryNode entryNode;

    /**
     * Mapping instruction index => list of associated exception-handlers.
     */
    private final Map<Integer, List<ExceptionHandler>> exceptionHandlers;

    /**
     * Mapping label => index of the label instruction.
     */
    private final Map<Label, Integer> label2index;

    /**
     * Mapping instruction index => instruction node.
     */
    private final Map<Integer, Node> nodes;

    /**
     * Creates new control flow graph based on given Java method.
     *
     * @param method Method, whose control flow graph is to be constructed.
     */
    public ControlFlowGraph(JavaMethod method) {
        // create necessary instance fields
        this.method = method;
        this.exceptionHandlers = new HashMap<Integer, List<ExceptionHandler>>();
        this.label2index = new HashMap<Label, Integer>();
        this.nodes = new HashMap<Integer, Node>();

        // create the entry and exit nodes
        this.entryNode = new EntryNode();

        // deal with methods without body
        if (this.method.getMethodNode().instructions.size() == 0) {
            System.err.println("Unexpected method without instructions: " +
                    this.method.getFullName());
            throw new InternalError();
        }

        // create mapping from labels to indices
        createLabelsIndices();

        // create nodes for every instruction
        createNodes();

        // create connections between the nodes
        connectNodes();
        this.entryNode.setSuccessor(this.nodes.get(0));

        updateAfterJsrSuccessors();
    }

    /**
     * Performs liveness analysis of the bytecode variables.
     */
    public void analyzeVariablesLiveness() {
        // Convention used: gen = use, kill = def

        // initialize the dataflow analysis
        initDataflowAnalysis();
        Set<Node> nodes = getAllNodes();
        for (Node node : nodes) {
            if (node instanceof SimpleNode) {
                SimpleNode simpleNode = (SimpleNode) node;
                switch (simpleNode.instruction.getOpcode()) {
                    case Opcodes.ALOAD:
                    case Opcodes.DLOAD:
                    case Opcodes.FLOAD:
                    case Opcodes.ILOAD:
                    case Opcodes.LLOAD:
                        VarInsnNode varInsnNode = (VarInsnNode) simpleNode.instruction;
                        node.addGenElement(varInsnNode.var);
                        break;

                    case Opcodes.ASTORE:
                    case Opcodes.DSTORE:
                    case Opcodes.FSTORE:
                    case Opcodes.ISTORE:
                    case Opcodes.LSTORE:
                        varInsnNode = (VarInsnNode) simpleNode.instruction;
                        node.addKillElement(varInsnNode.var);
                        break;

                    case Opcodes.IINC:
                        IincInsnNode iincInsnNode = (IincInsnNode) simpleNode.instruction;
                        node.addGenElement(iincInsnNode.var);
                        node.addKillElement(iincInsnNode.var);
                        break;
                }
            } else if (node instanceof RetNode) {
                RetNode retNode = (RetNode) node;
                VarInsnNode varInsnNode = (VarInsnNode) retNode.instruction;
                node.addGenElement(varInsnNode.var);
            }
        }

        // perform the dataflow analysis
        backwardUnionDataflowAnalysis();

        // set the results
        for (Node node : nodes) {
            node.setLiveVariables(node.getInSet());
        }

        // shutdown the dataflow analysis
        shutdownDataflowAnalysis();
    }

    /**
     * Performs a backward dataflow analysis with union as a confluence
     * operator.  Dataflow analysis has to be initialized and the 'gen' and
     * 'kill' sets have to be set appropriately.
     */
    private void backwardUnionDataflowAnalysis() {
        // calculate dataflow predecessors for each node
        Map<Node, Set<Node>> node2pred = new HashMap<Node, Set<Node>>();
        Set<Node> allNodes = getAllNodes();
        for (Node node : allNodes) {
            if (!node2pred.containsKey(node)) {
                node2pred.put(node, new HashSet<Node>());
            }
            for (Node succ : node.getAllDataflowSuccessors()) {
                if (!node2pred.containsKey(succ)) {
                    node2pred.put(succ, new HashSet<Node>());
                }
                node2pred.get(succ).add(node);
            }
        }

        // perform the analysis iteratively until fixed point is reached
        Queue<Node> nodesToUpdate = new LinkedList<Node>();
        Set<Node> nodesToUpdateSet = new HashSet<Node>();
        nodesToUpdate.addAll(allNodes);
        nodesToUpdateSet.addAll(nodesToUpdate);
        while (!nodesToUpdate.isEmpty()) {
            Node node = nodesToUpdate.poll();
            nodesToUpdateSet.remove(node);
            Set<Integer> newOut = new HashSet<Integer>();
            for (Node succ : node.getAllDataflowSuccessors()) {
                newOut.addAll(succ.getInSet());
            }
            Set<Integer> newIn = new HashSet<Integer>();
            newIn.addAll(newOut);
            newIn.removeAll(node.getKillSet());
            newIn.addAll(node.getGenSet());
            if (!newIn.equals(node.getInSet())) {
                node.setInSet(newIn);
                for (Node pred : node2pred.get(node)) {
                    if (!nodesToUpdateSet.contains(pred)) {
                        nodesToUpdate.add(pred);
                        nodesToUpdateSet.add(pred);
                    }
                }
            }
            if (!newOut.equals(node.getOutSet())) {
                node.setOutSet(newOut);
            }
        }
    }

    @SuppressWarnings("unused")
    private void oldAndSlowDataflowAnalysis() {
        Set<Node> nodes = getAllNodes();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Node node : nodes) {
                Set<Integer> newIn = new HashSet<Integer>();
                newIn.addAll(node.getOutSet());
                newIn.removeAll(node.getKillSet());
                newIn.addAll(node.getGenSet());
                Set<Integer> newOut = new HashSet<Integer>();
                for (Node succ : node.getAllDataflowSuccessors()) {
                    newOut.addAll(succ.getInSet());
                }
                if (!newIn.equals(node.getInSet()) ||
                        !newOut.equals(node.getOutSet())) {
                    node.setInSet(newIn);
                    node.setOutSet(newOut);
                    changed = true;
                }
            }
        }
    }

    /**
     * Initializes the dataflow analysis by cleaning up all the sets.
     */
    private void initDataflowAnalysis() {
        Set<Node> nodes = getAllNodes();
        for (Node node : nodes) {
            node.clearDataflowSets();
        }
    }

    /**
     * Shuts down the dataflow analysis by cleaning up all the sets.
     */
    private void shutdownDataflowAnalysis() {
        Set<Node> nodes = getAllNodes();
        for (Node node : nodes) {
            node.clearDataflowSets();
        }
    }

    /**
     * Returns all nodes in this control-flow graph.
     *
     * @return All nodes in this control-flow graph.
     */
    private Set<Node> getAllNodes() {
        Set<Node> allNodes = new HashSet<Node>();
        Queue<Node> nodesToAnalyze = new LinkedList<Node>();
        nodesToAnalyze.add(this.entryNode);
        while (!nodesToAnalyze.isEmpty()) {
            Node node = nodesToAnalyze.poll();
            if (allNodes.contains(node)) {
                continue;
            }
            allNodes.add(node);
            for (Node succ : node.getAllSuccessors()) {
                nodesToAnalyze.add(succ);
            }
        }
        return allNodes;
    }

    /**
     * Clears unnecessary fields to enable garbage collecting.  Calling this method
     * makes it impossible to change the control flow graph (for instance to inline
     * subroutines), but it is not checked, whether this is the case.
     * It is users' responsibility to not try to change the control flow graph after
     * freezing.
     */
    public void freeze() {
        // clear unnecessary fields to enable garbage collecting
        this.exceptionHandlers.clear();
        this.label2index.clear();
        this.nodes.clear();
    }

    /**
     * Returns entry node of this control flow graph.
     *
     * @return Entry node of this control flow graph.
     */
    public Node getEntryNode() {
        return this.entryNode;
    }

    /**
     * Inlines all subroutines in the places, where they are called.  This does
     * not remove jsr operations from the bytecode.  They will be needed later to
     * verify that inlined subroutines indeed return to their callers.  Inlining
     * means making copies of all nodes on all paths between a subroutine entry
     * and exit (excluding exceptional paths).
     */
    public void inlineSubroutines() {
        // do a copy of a subroutine for every call to a subroutine
        for (Node node : this.nodes.values()) {
            if (node instanceof JsrNode) {
                JsrNode jsrNode = (JsrNode) node;
                Node oldSuccessor = jsrNode.getJsrSuccessor();
                Node newSuccessor = copyNormalPath(oldSuccessor);
                jsrNode.setJsrSuccessor(newSuccessor);
            }
        }

        // update after jsr successors
        updateAfterJsrSuccessors();
    }

    /**
     * Creates nodes for every instruction in the method, whose control flow graph
     * is being created.  Nodes are not connected to each other.
     */
    private void createNodes() {
        // loop over all instruction
        MethodNode methodNode = this.method.getMethodNode();
        int currentLine = 0;
        for (int index = 0; index < methodNode.instructions.size(); index++) {

            // create the node according to the instruction at given index
            AbstractInsnNode insn =
                    (AbstractInsnNode) methodNode.instructions.get(index);
            Node node;

            // deal with special nodes' types
            if (insn instanceof FrameNode) {
                node = new EmptyNode(insn, index, currentLine);
            } else if (insn instanceof LabelNode) {
                node = new EmptyNode(insn, index, currentLine);
            } else if (insn instanceof LineNumberNode) {
                LineNumberNode lineNode = (LineNumberNode) insn;
                currentLine = lineNode.line;
                node = new EmptyNode(insn, index, currentLine);
            } else {
                // assume we have an instruction node, but double-check
                // its opcode in the default case
                switch (insn.getOpcode()) {
                    case Opcodes.ATHROW:
                        node = new ThrowNode(insn, index, currentLine);
                        break;

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
                        node = new ComparisonNode(insn, index, currentLine);
                        break;

                    case Opcodes.JSR:
                        node = new JsrNode(insn, index, currentLine);
                        break;

                    case Opcodes.LOOKUPSWITCH:
                    case Opcodes.TABLESWITCH:
                        node = new SwitchNode(insn, index, currentLine);
                        break;

                    case Opcodes.RET:
                        node = new RetNode(insn, index, currentLine);
                        break;

                    default:
                        if (insn.getOpcode() == -1) {
                            System.err.println("Encountered unexpected " +
                                    "instruction: " + insn);
                            throw new InternalError();
                        }
                        node = new SimpleNode(insn, index, currentLine);
                        break;
                }
            }

            // put the node into the node map
            this.nodes.put(index, node);
        }
    }

    /**
     * Creates connections between nodes to create a control flow graph.  Label
     * nodes are replaced by their successors.
     */
    private void connectNodes() {
        // prepare map of exception handlers for every instruction
        createExceptionHandlersMap();

        // create connections between nodes
        MethodNode methodNode = this.method.getMethodNode();
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            createNodeConnections(i);
        }
    }

    /**
     * Creates connections for a node representing instruction of given index in
     * a represented method.
     *
     * @param index Index of the instruction, whose node's connections are to be
     *              created.
     */
    private void createNodeConnections(int index) {
        // get the method node
        MethodNode methodNode = this.method.getMethodNode();

        // get the node for this instruction
        Node thisNode = this.nodes.get(index);

        // get the node for the next instruction, if it exists
        Node nextNode = null;
        if (index + 1 < methodNode.instructions.size()) {
            nextNode = this.nodes.get(index + 1);
        }

        // create the node according to the node type and the instruction at
        // given index
        if (thisNode instanceof EmptyNode) {
            EmptyNode emptyNode = (EmptyNode) thisNode;
            if (nextNode != null) {
                emptyNode.setSuccessor(nextNode);
            }
        } else if (thisNode instanceof BytecodeNode) {
            // get exception handlers for the instruction being processed
            List<ExceptionHandler> handlers = this.exceptionHandlers.get(index);

            AbstractInsnNode insn =
                    (AbstractInsnNode) methodNode.instructions.get(index);
            switch (insn.getOpcode()) {
                case Opcodes.AALOAD:
                case Opcodes.AASTORE:
                case Opcodes.ANEWARRAY:
                case Opcodes.ARRAYLENGTH:
                case Opcodes.BALOAD:
                case Opcodes.BASTORE:
                case Opcodes.CALOAD:
                case Opcodes.CASTORE:
                case Opcodes.CHECKCAST:
                case Opcodes.DALOAD:
                case Opcodes.DASTORE:
                case Opcodes.FALOAD:
                case Opcodes.FASTORE:
                case Opcodes.GETFIELD:
                case Opcodes.IALOAD:
                case Opcodes.IASTORE:
                case Opcodes.IDIV:
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.IREM:
                case Opcodes.LALOAD:
                case Opcodes.LASTORE:
                case Opcodes.LDIV:
                case Opcodes.LREM:
                case Opcodes.MULTIANEWARRAY:
                case Opcodes.NEWARRAY:
                case Opcodes.PUTFIELD:
                case Opcodes.SALOAD:
                case Opcodes.SASTORE:
                    SimpleNode simpleNode = (SimpleNode) thisNode;
                    simpleNode.setSuccessor(nextNode);
                    addExceptionHandlers(simpleNode, handlers);
                    break;

                case Opcodes.ARETURN:
                case Opcodes.DRETURN:
                case Opcodes.FRETURN:
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.RETURN:
                    simpleNode = (SimpleNode) thisNode;
                    break;

                case Opcodes.ATHROW:
                    ThrowNode throwNode = (ThrowNode) thisNode;
                    addExceptionHandlers(throwNode, handlers);
                    break;

                case Opcodes.GOTO:
                    simpleNode = (SimpleNode) thisNode;
                    int jumpIndex = this.label2index.get(((JumpInsnNode) insn).label.getLabel());
                    simpleNode.setSuccessor(this.nodes.get(jumpIndex));
                    break;

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
                    ComparisonNode comparisonNode = (ComparisonNode) thisNode;
                    jumpIndex = this.label2index.get(((JumpInsnNode) insn).label.getLabel());
                    comparisonNode.setFalseSuccessor(nextNode);
                    comparisonNode.setTrueSuccessor(this.nodes.get(jumpIndex));
                    break;

                case Opcodes.JSR:
                    JsrNode jsrNode = (JsrNode) thisNode;
                    jumpIndex = this.label2index.get(((JumpInsnNode) insn).label.getLabel());
                    jsrNode.setJsrSuccessor(this.nodes.get(jumpIndex));
                    jsrNode.setRetSuccessor(nextNode);
                    break;

                case Opcodes.LOOKUPSWITCH:
                    SwitchNode switchNode = (SwitchNode) thisNode;
                    LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) insn;
                    int dfltIndex = this.label2index.get(lookupSwitchInsn.dflt.getLabel());
                    switchNode.setDefaultSuccessor(this.nodes.get(dfltIndex));
                    for (int i = 0; i < lookupSwitchInsn.keys.size(); i++) {
                        Integer key = (Integer) lookupSwitchInsn.keys.get(i);
                        Label label = ((LabelNode) lookupSwitchInsn.labels.get(i)).getLabel();
                        int labelIndex = this.label2index.get(label);
                        Node keySuccessor = this.nodes.get(labelIndex);
                        switchNode.addKeySuccessor(key, keySuccessor);
                    }
                    break;

                case Opcodes.TABLESWITCH:
                    switchNode = (SwitchNode) thisNode;
                    TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) insn;
                    dfltIndex = this.label2index.get(tableSwitchInsn.dflt.getLabel());
                    switchNode.setDefaultSuccessor(this.nodes.get(dfltIndex));
                    for (int i = 0; i < tableSwitchInsn.labels.size(); i++) {
                        Integer key = tableSwitchInsn.min + i;
                        Label label = ((LabelNode) tableSwitchInsn.labels.get(i)).getLabel();
                        int labelIndex = this.label2index.get(label);
                        Node keySuccessor = this.nodes.get(labelIndex);
                        switchNode.addKeySuccessor(key, keySuccessor);
                    }
                    break;

                case Opcodes.RET:
                    // no connections - do nothing
                    break;

                default:
                    simpleNode = (SimpleNode) thisNode;
                    simpleNode.setSuccessor(nextNode);
                    break;
            }
        } else {
            System.err.println("Unexpected node type: " +
                    thisNode.getClass().toString());
            throw new InternalError();
        }
    }

    /**
     * Adds successor "exceptional" nodes to the given node for each of the
     * exception handlers associated with this node.
     *
     * @param node     Node to which successors are to be added.
     * @param handlers List of exception handlers.
     */
    private void addExceptionHandlers(BytecodeNode node,
                                      List<ExceptionHandler> handlers) {
        for (int i = 0; i < handlers.size(); i++) {
            node.addExceptionHandler(handlers.get(i));
        }
    }

    /**
     * Creates internal mapping from instructions indices to list of all handlers
     * of exceptions associated with those indices.
     */
    private void createExceptionHandlersMap() {
        // get the method node
        MethodNode methodNode = this.method.getMethodNode();

        // create empty lists of exception handlers for every instruction
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            this.exceptionHandlers.put(i, new ArrayList<ExceptionHandler>());
        }

        // update maps for instructions covered by each exception handler
        for (Object tryCatchBlockObject : methodNode.tryCatchBlocks) {
            // extract data about the exception
            TryCatchBlockNode tryCatchBlockNode =
                    (TryCatchBlockNode) tryCatchBlockObject;
            int startIndex = this.label2index.get(tryCatchBlockNode.start.getLabel());
            int endIndex = this.label2index.get(tryCatchBlockNode.end.getLabel());
            int handlerIndex = this.label2index.get(tryCatchBlockNode.handler.getLabel());
            String excType;
            if (tryCatchBlockNode.type == null) {
                excType = null;
            } else {
                excType = tryCatchBlockNode.type.replace('/', '.');
            }
            Node handlerNode = this.nodes.get(handlerIndex);

            // add information about this exception to all covered instructions
            ExceptionHandler handler = new ExceptionHandler(excType, handlerNode);
            for (int i = startIndex; i < endIndex; i++) {
                this.exceptionHandlers.get(i).add(handler);
            }
        }
    }

    /**
     * Creates indices of all labels in the method, whose cfg is represented.
     */
    private void createLabelsIndices() {
        MethodNode methodNode = this.method.getMethodNode();
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            Object insnObject = methodNode.instructions.get(i);
            AbstractInsnNode insnNode = (AbstractInsnNode) insnObject;
            if (insnNode.getType() == AbstractInsnNode.LABEL) {
                LabelNode labelNode = (LabelNode) insnNode;
                this.label2index.put(labelNode.getLabel(), i);
            }
        }
    }

    /**
     * Copies a path starting at a given entry node and consisting of this node
     * and all its (transitive as well) "normal" successors.
     *
     * @param entry Entry node of a path to copy.
     * @return Entry node to a copied path.
     */
    private Node copyNormalPath(Node entry) {
        Map<Node, Node> copies = new HashMap<Node, Node>();
        Node result = entry.copyNormalPath(copies);
        copies.clear();
        return result;
    }

    /**
     * Updates after-jsr successors of RET nodes.
     */
    private void updateAfterJsrSuccessors() {
        // find all after-jsr nodes
        Set<Node> afterJsrs = new HashSet<Node>();
        Set<Node> nodes = getAllNodes();
        for (Node node : nodes) {
            if (node instanceof JsrNode) {
                JsrNode jsrNode = (JsrNode) node;
                afterJsrs.add(jsrNode.getRetSuccessor());
            }
        }

        // update after-jsr successors of RET nodes
        for (Node node : nodes) {
            if (node instanceof RetNode) {
                RetNode retNode = (RetNode) node;
                retNode.setAfterJsrSuccessors(afterJsrs);
            }
        }
    }
}
