package org.softevo.oumextractor;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.softevo.oumextractor.analysis.Analyzer;
import org.softevo.oumextractor.controlflow.ControlFlowGraph;
import org.softevo.oumextractor.controlflow.Node;

import java.util.*;

/**
 * This class is responsible for representing Java methods.
 *
 * @author Andrzej Wasylkowski
 */
public final class JavaMethod {

    /**
     * Mapping fully qualified method name => sole return variable.
     */
    private static final Map<String, String> method2return;

    /**
     * Code that initializes method2returns.
     */
    static {
        method2return = new HashMap<String, String>();

        method2return.put("java.lang.StringBuffer.append(Z)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(C)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append([C)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append([CII)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(Ljava/lang/CharSequence;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(Ljava/lang/CharSequence;II)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(D)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(F)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(I)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(J)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(Ljava/lang/Object;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(Ljava/lang/String;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.append(Ljava/lang/StringBuffer;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.appendCodePoint(I)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.delete(II)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.deleteCharAt(I)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(IZ)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(IC)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(I[C)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(I[CII)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(ILjava/lang/CharSequence;II)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(ID)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(IF)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(II)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(IJ)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(ILjava/lang/Object;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.insert(ILjava/lang/String;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.replace(IILjava/lang/String;)Ljava/lang/StringBuffer;", "THIS");
        method2return.put("java.lang.StringBuffer.reverse()Ljava/lang/StringBuffer;", "THIS");

        method2return.clear();
    }

    /**
     * Type, in which the represented method is defined.
     */
    private final JavaType type;
    /**
     * Bytecode representation of the represented method.
     */
    private final MethodNode methodNode;
    /**
     * Full (class name + method name + descriptor) name of this method.
     */
    private final String fullName;
    /**
     * Name + descriptor of this method.
     */
    private final String name;
    /**
     * List of fully qualified names of exceptions thrown by the method.
     */
    private final List<String> exceptions;
    /**
     * Access flags of this method.
     */
    private final int access;
    /**
     * Line number of the first instruction of this method.
     */
    private final int firstLineNumber;
    /**
     * Control flow graph representation of the represented method.
     */
    private ControlFlowGraph cfg;

    /**
     * Creates representation of the Java method based on its bytecode
     * representation.
     *
     * @param clas       Class, in which the method to represent is defined.
     * @param methodNode Bytecode representation of the method to represent.
     * @param getBody    If <code>true</code> the body will be stored;
     *                   otherwise not.
     */
    public JavaMethod(JavaType type, MethodNode methodNode, boolean getBody) {
        // initialize fields
        this.type = type;
        this.name = methodNode.name + methodNode.desc;
        this.fullName = this.type.getFullName() + '.' + this.name;
        this.exceptions = new ArrayList<String>();
        for (Object excObject : methodNode.exceptions) {
            String exc = ((String) excObject).replace('/', '.');
            this.exceptions.add(exc);
        }
        this.access = methodNode.access;
        if (getBody) {
            this.methodNode = methodNode;
        } else {
            this.methodNode = null;
        }
        this.cfg = null;
        int line = 0;
        for (int index = 0; index < methodNode.instructions.size(); index++) {
            AbstractInsnNode insn =
                    (AbstractInsnNode) methodNode.instructions.get(index);
            if (insn instanceof LineNumberNode) {
                LineNumberNode lineNode = (LineNumberNode) insn;
                line = lineNode.line;
                break;
            }
        }
        this.firstLineNumber = line;
    }

    /**
     * Returns the line number of the first instruction in this method.
     *
     * @return Line number of the first instruction in this method.
     */
    public int getFirstLineNumber() {
        return this.firstLineNumber;
    }

    /**
     * Returns full name of this method, which consists of the type name,
     * method name and method signature.
     *
     * @return Full name of the represented method.
     */
    public String getFullName() {
        return this.fullName;
    }

    /**
     * Returns unique name of this method, which consists of the method name and
     * its signature.
     *
     * @return Unique name of the represented method.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns list of exceptions thrown by this method.
     *
     * @return List of exceptions thrown by this method.
     */
    public List<String> getExceptions() {
        return Collections.unmodifiableList(this.exceptions);
    }

    /**
     * Returns access flags of this method.
     *
     * @return Access flags of this method.
     */
    public int getAccess() {
        return this.access;
    }

    /**
     * Returns bytecode representation of the represented method.
     *
     * @return Bytecode represenation of the represented method.
     */
    public MethodNode getMethodNode() {
        if (this.methodNode == null) {
            System.err.println("[ERROR] No node for method: " + this.fullName);
            throw new InternalError();
        }
        return this.methodNode;
    }

    /**
     * Returns representation of the type that defines this method.
     *
     * @return Representation of the type that defines this method.
     */
    public JavaType getJavaType() {
        return this.type;
    }

    /**
     * Indicates, whether call to the represented method always results in it
     * returning the same variable: its object or one of its parameters.
     *
     * @return <code>true</code> if call to the represented method always
     * results in it returning the same variable; <code>false</code>
     * otherwise.
     */
    public boolean alwaysReturnsTheSameVariable() {
        return method2return.containsKey(getFullName());
    }

    /**
     * Indicates, whether call to the represented method always results in it
     * returning given parameter.
     *
     * @param i Index of a parameter with 1 being the first parameter.
     * @return <code>true</code> if call to the represented method always
     * results in it returning given parameter; <code>false</code>
     * otherwise.
     */
    public boolean alwaysReturnsParameter(int i) {
        if (method2return.get(getFullName()).equals("PARAM #" + i)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Indicates, whether call to the represented method always results in it
     * returning its object.
     *
     * @return <code>true</code> if call to the represented method always
     * results in it returning its object; <code>false</code>
     * otherwise.
     */
    public boolean alwaysReturnsThis() {
        if (method2return.get(getFullName()).equals("THIS")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates control flow graph of the represented method.
     */
    void createCFG() {
        this.cfg = new ControlFlowGraph(this);
        this.cfg.inlineSubroutines();
        this.cfg.freeze();
        this.cfg.analyzeVariablesLiveness();
    }

    /**
     * Performs data flow analysis of this method.
     *
     * @param analyzer Analyzer to be used.
     */
    void analyzeDataFlow(Analyzer analyzer) {
        // start the analysis
        analyzer.startAnalysis(this);

        // create depth-first list of all nodes in a control flow graph
        LinkedList<Node> nodesToProcess = new LinkedList<Node>();
        List<Node> nodes = new ArrayList<Node>();
        Set<Node> visitedNodes = new HashSet<Node>();
        nodesToProcess.add(this.cfg.getEntryNode());
        while (!nodesToProcess.isEmpty()) {
            Node node = nodesToProcess.removeLast();
            if (visitedNodes.contains(node)) {
                continue;
            }
            visitedNodes.add(node);
            nodes.add(node);
            for (Node next : node.getAllSuccessors()) {
                nodesToProcess.addLast(next);
            }
        }

        // initialize the state of the entry node
        this.cfg.getEntryNode().setIn(analyzer.getEntryFrame());

        // analyze the method
        analyzer.analyze(this.cfg.getEntryNode());
        analyzer.endAnalysis();

        // dispose of the control flow graph
        this.cfg = null;
    }
}
