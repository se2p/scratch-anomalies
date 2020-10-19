package org.softevo.oumextractor.modelcreator1;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.softevo.oumextractor.JavaClass;
import org.softevo.oumextractor.JavaClassPool;
import org.softevo.oumextractor.JavaMethod;
import org.softevo.oumextractor.JavaType;
import org.softevo.oumextractor.analysis.AnalyzeErrorException;
import org.softevo.oumextractor.analysis.Frame;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;
import org.softevo.oumextractor.controlflow.*;
import org.softevo.oumextractor.modelcreator1.model.Model;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used as a data flow analyzer producing object usage models.
 *
 * @author Andrzej Wasylkowski
 */
public final class Analyzer extends org.softevo.oumextractor.analysis.Analyzer {

    /**
     * Logger to be used by this class.
     */
    private final static Logger logger =
            Logger.getLogger("org.softevo.oumextractor.modelcreator1");

    static {
        Analyzer.logger.setLevel(Analyzer.logger.getParent().getLevel());
    }

    /**
     * The null value.
     */
    private final NullValue nullValue;
    /**
     * Directory to store the models into.
     */
    private final String modelsDir;
    /**
     * Names of all types, that will be processed by this Analyzer.
     */
    private final Set<String> typesNames;
    /**
     * Mapping model id number => model data.
     */
    private final Map<Integer, ModelData> id2modelData;

    /**
     * Mapping model name => model for models of the currently processed method.
     */
    private final Map<String, Model> currentModels;

    /**
     * Models to serialize during next serialization.
     */
    private final Set<Model> modelsToSerialize;

    /**
     * Mapping model => model id.
     */
    private final Map<Model, Integer> model2id;

    /**
     * Mapping node => model name for object created at that node.
     */
    private final Map<Node, String> nodeModels;
    /**
     * Number of models created by this analyzer. Used to generate model id.
     */
    private int modelsCreated;
    /**
     * Node which is currently being analyzed.
     */
    private Node activeNode;

    /**
     * Creates new instance of this analyzer.
     *
     * @param modelsDir  Directory to hold the models.
     * @param typesNames Names of all types that will be processed.
     */
    public Analyzer(String modelsDir, Set<String> typesNames) {
        this.nullValue = NullValue.getInstance();
        this.modelsDir = modelsDir;
        this.typesNames = new HashSet<String>(typesNames);
        this.id2modelData = new HashMap<Integer, ModelData>();
        this.nodeModels = new HashMap<Node, String>();
        this.model2id = new HashMap<Model, Integer>();
        this.currentModels = new HashMap<String, Model>();
        this.modelsToSerialize = new HashSet<Model>();
        this.modelsCreated = 0;

        // empty models dir
        File destDir = new File(this.modelsDir);
        destDir.mkdirs();
        for (File file : destDir.listFiles()) {
            file.delete();
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#startAnalysis(org.softevo.oumextractor.JavaMethod)
     */
    @Override
    public void startAnalysis(JavaMethod method) {
        super.startAnalysis(method);

        for (Model model : this.currentModels.values()) {
            model.clear();
        }
        this.currentModels.clear();
        this.nodeModels.clear();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#endAnalysis()
     */
    @Override
    public void endAnalysis() {
        super.endAnalysis();

        // get the class name and the method name
        String className = this.javaMethod.getJavaType().getFullName();
        String methodName = this.javaMethod.getName();

        // assign ids and data to models
        for (String modelName : this.currentModels.keySet()) {
            Model model = this.currentModels.get(modelName);
            this.modelsCreated++;
            this.model2id.put(model, this.modelsCreated);
            this.id2modelData.put(this.modelsCreated,
                    new ModelData(className, methodName, modelName));
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#shutdownAnalysis()
     */
    @Override
    public void shutdownAnalysis() {
        super.shutdownAnalysis();

        // serialize models info
        File targetDirectory = new File(this.modelsDir);
        targetDirectory.mkdirs();
        try {
            String fileName = "modelsdata.ser";
            BufferedOutputStream fileOutput = new BufferedOutputStream(
                    new FileOutputStream(new File(targetDirectory, fileName)));
            ObjectOutputStream objectOutput =
                    new ObjectOutputStream(fileOutput);
            writeId2ModelData(objectOutput);
            objectOutput.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }

        // serialize names of types investigated
        try {
            String fileName = "typesnames.ser";
            BufferedOutputStream fileOutput = new BufferedOutputStream(
                    new FileOutputStream(new File(targetDirectory, fileName)));
            ObjectOutputStream objectOutput =
                    new ObjectOutputStream(fileOutput);
            writeTypesNames(objectOutput);
            objectOutput.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }

        // create index file
        PrintStream ps = null;
        try {
            ps = new PrintStream(new File(targetDirectory, "index.txt"));
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Couldn't create index.txt in " +
                    targetDirectory);
            return;
        }

        // fill the index
        List<Integer> ids =
                new ArrayList<Integer>(this.id2modelData.keySet());
        System.out.println(ids.size() + " MODELS EXTRACTED");
        Collections.sort(ids);
        for (Integer id : ids) {
            ModelData modelData = this.id2modelData.get(id);

            StringBuffer description = new StringBuffer();
            description.append("INDEX:  ").append(id).append("\n");
            description.append("MODEL:  ").append(modelData.getModelName());
            description.append("\n");
            description.append("CLASS:  ").append(modelData.getClassName());
            description.append("\n");
            description.append("METHOD: ").append(modelData.getMethodName());
            description.append("\n");
            ps.println("--------------------------------------------------");
            ps.print(description);
            ps.println("--------------------------------------------------");
            ps.println();
        }
        ps.close();
    }

    /**
     * Serializes models created since the last serialization (or since the
     * beginning of the analysis, if this is the first serialization) until
     * this point to a file.  This must be called after finishing analysis of
     * every class, because models are serialized in files according to classes,
     * from which they were created.
     */
    public void serializeModels() {
        // serialize models if necessary
        if (this.modelsToSerialize.size() > 0) {
            File targetDirectory = new File(this.modelsDir);
            try {
                String fileName =
                        this.javaMethod.getJavaType().getFullName() +
                                ".models.ser";
                BufferedOutputStream fileOutput = new BufferedOutputStream(
                        new FileOutputStream(new File(targetDirectory, fileName),
                                true));
                ObjectOutputStream objectOutput =
                        new ObjectOutputStream(fileOutput);
                objectOutput.writeInt(this.modelsToSerialize.size());
                for (Model model : this.modelsToSerialize) {
                    model.minimize();
                    objectOutput.writeInt(this.model2id.get(model));
                    System.out.println(model);
                    objectOutput.writeObject(model);
                }
                objectOutput.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace(System.err);
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                System.exit(0);
            }
        }

        // empty the set of models to be serialized
        this.modelsToSerialize.clear();
        this.model2id.clear();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#preAnalyzeNode(org.softevo.oumextractor.controlflow.Node, org.softevo.oumextractor.analysis.Frame)
     */
    @Override
    public void preAnalyzeNode(Node node, Frame frame) {
        super.preAnalyzeNode(node, frame);

        this.activeNode = node;
        frame.initializeEnteringValues();

        // log information about current frame
        if (Analyzer.logger.getLevel().intValue() <= Level.FINER.intValue()) {
            Analyzer.logger.finer("About to analyze node: " + node.toString() + "\n");
            Analyzer.logger.finer(frame.toString() + "\n");
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#postAnalyzeNode(org.softevo.oumextractor.controlflow.Node)
     */
    @Override
    public void postAnalyzeNode(Node node) {
        super.postAnalyzeNode(node);

        // log information about end of analysis
        if (Analyzer.logger.getLevel().intValue() <= Level.FINER.intValue()) {
            Analyzer.logger.finer("Ended analyzing node: " +
                    this.activeNode.toString() + "\n");
            if (this.activeNode.getOut() == null) {
                Analyzer.logger.finer("No \"normal\" output frame\n");
            } else {
                Analyzer.logger.finer(this.activeNode.getOut().toString() +
                        "\n");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#markFrameAsAnalyzed(org.softevo.oumextractor.analysis.Frame, org.softevo.oumextractor.controlflow.Node)
     */
    @Override
    public void markFrameAsAnalyzed(Frame frame, Node successor) {
        // remove all dead variables
        frame.retainVariables(successor.getLiveVariables());

        // notify all objects about a node that will be analyzed next
        List<Value> frameValues = frame.getFrameValues();
        for (Value in_value : frameValues) {
            if (in_value instanceof AValue) {
                AValue value = (AValue) in_value;
                Set<ModelableAValue> mavalues = value.getModelableValues();
                for (ModelableAValue mavalue : mavalues) {
                    mavalue.setCurrentNode(successor);
                }
            }
        }

        // notify objects that disappeared about the fact
        Set<Value> disappeared = frame.getDisappearedValues();
        for (Value in_value : disappeared) {
            if (in_value instanceof AValue) {
                AValue value = (AValue) in_value;
                Set<ModelableAValue> mavalues = value.getModelableValues();
                for (ModelableAValue mavalue : mavalues) {
                    mavalue.returnFromMethod(null);
                }
            }
        }

        frame.updateValuesStructure();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#rollbackAnalysis()
     */
    @Override
    public void rollbackAnalysis() {
        super.rollbackAnalysis();

        this.modelsToSerialize.removeAll(this.currentModels.values());
        for (Model model : this.currentModels.values()) {
            this.model2id.remove(model);
            model.clear();
        }

        this.currentModels.clear();
        this.nodeModels.clear();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#getInitFrame()
     */
    @Override
    public Frame getInitFrame() {
        Frame result = new Frame(this.javaMethod.getMethodNode().maxLocals);
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#getEntryFrame()
     */
    @Override
    public Frame getEntryFrame() {
        // create the frame
        MethodNode methodNode = this.javaMethod.getMethodNode();
        Frame result = new Frame(methodNode.maxLocals);

        // determine index of the first parameter
        int parametersStart = 1;
        if ((methodNode.access & Opcodes.ACC_STATIC) != 0) {
            parametersStart = 0;
        }
        // create value for 'this' if necessary
        else {
            String className = this.javaMethod.getJavaType().getFullName();
            ObjectValue thisValue = newObjectValue(className, true);
            String thisValueName = "this@" + className + " (line " +
                    this.javaMethod.getFirstLineNumber() + ")";

            Model model = getModel(thisValueName);
            thisValue.enableModeling(model, thisValueName);
            result.setLocalVariable(0, thisValue);
        }

        // create values for all parameters
        Type[] params = Type.getArgumentTypes(methodNode.desc);
        int gapOffset = 0;
        for (int i = 0; i < params.length; i++) {
            Value paramValue = newValue(params[i], false);
            if (paramValue instanceof ObjectValue) {
                ModelableAValue value = (ModelableAValue) paramValue;
                String valueName = "param #" + String.valueOf(i + 1) + "@" +
                        value.getTypeName() + " (line " +
                        this.javaMethod.getFirstLineNumber() + ")";
                Model model = getModel(valueName);
                value.enableModeling(model, valueName);
            }
            result.setLocalVariable(parametersStart + i + gapOffset,
                    paramValue);

            // make the necessary gap for values of size 2
            if (paramValue.getSize() == 2) {
                gapOffset++;
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#returnFromMethod(org.softevo.oumextractor.analysis.Frame, org.softevo.oumextractor.analysis.Value)
     */
    @Override
    public void returnFromMethod(Frame frame, Value value) {
        super.returnFromMethod(frame, value);

        // notify all objects about the fact of returning
        List<Value> frameValues = frame.getFrameValues();
        for (Value in_value : frameValues) {
            if (in_value instanceof AValue) {
                AValue v = (AValue) in_value;
                Set<ModelableAValue> mavalues = v.getModelableValues();
                for (ModelableAValue mavalue : mavalues) {
                    mavalue.returnFromMethod(value);
                }
            }
        }
        if (value != null) {
            for (ModelableAValue mavalue : ((AValue) value).getModelableValues()) {
                mavalue.returnFromMethod(value);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#unhandledException(java.lang.String, org.softevo.oumextractor.analysis.Frame)
     */
    @Override
    public void unhandledException(String excType, Frame out) {
        super.unhandledException(excType, out);

        // notify all objects about the fact of exception
        List<Value> frameValues = out.getFrameValues();
        for (Value in_value : frameValues) {
            if (in_value instanceof AValue) {
                AValue v = (AValue) in_value;
                Set<ModelableAValue> mavalues = v.getModelableValues();
                for (ModelableAValue mavalue : mavalues) {
                    mavalue.unhandledException(excType);
                }
            }
        }

        // notify objects that disappeared about the fact
        Set<Value> disappeared = out.getDisappearedValues();
        for (Value in_value : disappeared) {
            if (in_value instanceof AValue) {
                AValue value = (AValue) in_value;
                Set<ModelableAValue> mavalues = value.getModelableValues();
                for (ModelableAValue mavalue : mavalues) {
                    mavalue.unhandledException(excType);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#performOperation(org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    public void performOperation(AbstractInsnNode node)
            throws AnalyzeErrorException {
        // prepare variables
        AValue value;
        AValue value1;
        AValue value2;
        AValue value3;
        List<AValue> values = new ArrayList<AValue>();

        // perform the operation
        switch (node.getOpcode()) {
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.DALOAD:
            case Opcodes.FALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.SALOAD:
                Frame frame = this.activeNode.getIn().copy();
                SimpleNode simpleNode = (SimpleNode) this.activeNode;
                value2 = (AValue) frame.popOperand();
                value1 = (AValue) frame.popOperand();

                if (value1.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    simpleNode.addException(excType, exc);
                }
                if (value1.canBeNonNull()) {
                    Frame successorFrame = simpleNode.getSuccessorFrame();
                    value2 = (AValue) successorFrame.popOperand();
                    value1 = (AValue) successorFrame.popOperand();

                    Set<ArrayValue> arrayValues = value1.getArrayValues(false);
                    MergesCache merges = new MergesCache();
                    Iterator<ArrayValue> arrayValuesIterator =
                            arrayValues.iterator();
                    AValue resultValue = arrayValuesIterator.next().
                            getArrayElement(value2);
                    while (arrayValuesIterator.hasNext()) {
                        ArrayValue arrayValue = arrayValuesIterator.next();
                        resultValue = arrayValue.getArrayElement(value2).
                                merge(resultValue, merges);
                    }

                    successorFrame.pushOperand(resultValue);
                }
                break;

            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.DASTORE:
            case Opcodes.FASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.SASTORE:
                frame = this.activeNode.getIn().copy();
                simpleNode = (SimpleNode) this.activeNode;
                value3 = (AValue) frame.popOperand();
                value2 = (AValue) frame.popOperand();
                value1 = (AValue) frame.popOperand();

                if (value1.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    simpleNode.addException(excType, exc);
                }
                if (value1.canBeNonNull()) {
                    Frame successorFrame = simpleNode.getSuccessorFrame();
                    value3 = (AValue) successorFrame.popOperand();
                    value2 = (AValue) successorFrame.popOperand();
                    value1 = (AValue) successorFrame.popOperand();

                    Set<ArrayValue> arrayValues = value1.getArrayValues(false);
                    for (ArrayValue array : arrayValues) {
                        array.setArrayElement(value2, value3);
                    }
                }
                break;

            case Opcodes.ANEWARRAY:
                Frame successorFrame =
                        ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();

                TypeInsnNode typeInsnNode = (TypeInsnNode) node;
                String descriptor = typeInsnNode.desc;
                if (!descriptor.startsWith("[")) {
                    descriptor = "L" + descriptor + ";";
                }
                AValue element = newValue(Type.getType(descriptor), false);
                Value arrayValue = new ArrayValue(element, value);

                successorFrame.pushOperand(arrayValue);
                break;

            case Opcodes.ARRAYLENGTH:
                frame = this.activeNode.getIn().copy();
                value = (AValue) frame.popOperand();

                if (value.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    ((SimpleNode) this.activeNode).addException(excType, exc);
                }
                if (value.canBeNonNull()) {
                    successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                    value = (AValue) successorFrame.popOperand();

                    MergesCache merges = new MergesCache();
                    Set<ArrayValue> arrayValues = value.getArrayValues(false);
                    Iterator<ArrayValue> arrayValuesIterator =
                            arrayValues.iterator();
                    AValue arraySize = arrayValuesIterator.next().getArraySize();
                    while (arrayValuesIterator.hasNext()) {
                        ArrayValue array = arrayValuesIterator.next();
                        arraySize = array.getArraySize().merge(arraySize, merges);
                    }

                    successorFrame.pushOperand(arraySize);
                }
                break;

            case Opcodes.ATHROW:
                frame = this.activeNode.getIn().copy();
                value = (AValue) frame.popOperand();

                if (value.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    ((ThrowNode) this.activeNode).addException(excType, exc);
                }
                if (value.canBeNonNull()) {
                    Set<ObjectValue> objectValues = value.getObjectRepresentations();
                    for (ObjectValue object : objectValues) {
                        String excType = object.getTypeName();
                        ((ThrowNode) this.activeNode).addException(excType, object);
                    }
                }
                break;

            case Opcodes.CHECKCAST:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();
                AValue resultValue;

                typeInsnNode = (TypeInsnNode) node;
                descriptor = typeInsnNode.desc;
                if (!descriptor.startsWith("[")) {
                    descriptor = "L" + descriptor + ";";
                }
                Type cast = Type.getType(descriptor);

                if (cast.getSort() == Type.ARRAY) {
                    Set<ModelableAValue> mavalues = value.getModelableValues();
                    for (ModelableAValue mavalue : mavalues) {
                        mavalue.returnFromMethod(null);
                        mavalue.disableModeling();
                    }
                    resultValue = newValue(cast, false);

                    // update the frame because some multiple values may have
                    // been distorted
                    successorFrame.updateValuesStructure();
                } else if (cast.getSort() == Type.OBJECT) {
                    Set<ObjectValue> objectValues = value.getObjectRepresentations();
                    for (ObjectValue object : objectValues) {
                        object.castToType(cast.getClassName());
                    }
                    resultValue = value;

                    // update the frame because some multiple values may have
                    // been distorted
                    successorFrame.updateValuesStructure();
                } else {
                    System.err.println("[ERROR] Unexpected type: " + cast);
                    throw new InternalError();
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.D2F:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.DNEG:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();

                Set<DoubleValue> doubleValues = value.getDoubleValues();
                Iterator<DoubleValue> doubleValuesIterator =
                        doubleValues.iterator();
                resultValue = doubleValuesIterator.next().
                        performOperation(node.getOpcode());
                MergesCache merges = new MergesCache();
                while (doubleValuesIterator.hasNext()) {
                    DoubleValue double_ = doubleValuesIterator.next();
                    resultValue = double_.performOperation(node.getOpcode()).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.DADD:
            case Opcodes.DMUL:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                doubleValues = value1.getDoubleValues();
                doubleValuesIterator = doubleValues.iterator();
                DoubleValue double_ = doubleValuesIterator.next();
                resultValue = double_.performCommutativeOperation(node.getOpcode(), value2);
                merges = new MergesCache();
                while (doubleValuesIterator.hasNext()) {
                    double_ = doubleValuesIterator.next();
                    resultValue = double_.performCommutativeOperation(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                doubleValues = value2.getDoubleValues();
                doubleValuesIterator = doubleValues.iterator();
                double_ = doubleValuesIterator.next();
                resultValue = double_.performCommutativeOperation(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (doubleValuesIterator.hasNext()) {
                    double_ = doubleValuesIterator.next();
                    resultValue = double_.performCommutativeOperation(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.DCMPG:
            case Opcodes.DCMPL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.DSUB:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                doubleValues = value1.getDoubleValues();
                doubleValuesIterator = doubleValues.iterator();
                double_ = doubleValuesIterator.next();
                resultValue = double_.performNonCommutativeOperationLeft(node.getOpcode(), value2);
                merges = new MergesCache();
                while (doubleValuesIterator.hasNext()) {
                    double_ = doubleValuesIterator.next();
                    resultValue = double_.performNonCommutativeOperationLeft(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                doubleValues = value2.getDoubleValues();
                doubleValuesIterator = doubleValues.iterator();
                double_ = doubleValuesIterator.next();
                resultValue = double_.performNonCommutativeOperationRight(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (doubleValuesIterator.hasNext()) {
                    double_ = doubleValuesIterator.next();
                    resultValue = double_.performNonCommutativeOperationRight(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.F2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.FNEG:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();

                Set<FloatValue> floatValues = value.getFloatValues();
                Iterator<FloatValue> floatValuesIterator =
                        floatValues.iterator();
                resultValue = floatValuesIterator.next().
                        performOperation(node.getOpcode());
                merges = new MergesCache();
                while (floatValuesIterator.hasNext()) {
                    FloatValue float_ = floatValuesIterator.next();
                    resultValue = float_.performOperation(node.getOpcode()).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.FADD:
            case Opcodes.FMUL:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                floatValues = value1.getFloatValues();
                floatValuesIterator = floatValues.iterator();
                FloatValue float_ = floatValuesIterator.next();
                resultValue = float_.performCommutativeOperation(node.getOpcode(), value2);
                merges = new MergesCache();
                while (floatValuesIterator.hasNext()) {
                    float_ = floatValuesIterator.next();
                    resultValue = float_.performCommutativeOperation(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                floatValues = value2.getFloatValues();
                floatValuesIterator = floatValues.iterator();
                float_ = floatValuesIterator.next();
                resultValue = float_.performCommutativeOperation(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (floatValuesIterator.hasNext()) {
                    float_ = floatValuesIterator.next();
                    resultValue = float_.performCommutativeOperation(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
            case Opcodes.FSUB:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                floatValues = value1.getFloatValues();
                floatValuesIterator = floatValues.iterator();
                float_ = floatValuesIterator.next();
                resultValue = float_.performNonCommutativeOperationLeft(node.getOpcode(), value2);
                merges = new MergesCache();
                while (floatValuesIterator.hasNext()) {
                    float_ = floatValuesIterator.next();
                    resultValue = float_.performNonCommutativeOperationLeft(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                floatValues = value2.getFloatValues();
                floatValuesIterator = floatValues.iterator();
                float_ = floatValuesIterator.next();
                resultValue = float_.performNonCommutativeOperationRight(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (floatValuesIterator.hasNext()) {
                    float_ = floatValuesIterator.next();
                    resultValue = float_.performNonCommutativeOperationRight(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.GETFIELD:
                frame = this.activeNode.getIn().copy();
                value = (AValue) frame.popOperand();
                if (value.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    ((SimpleNode) this.activeNode).addException(excType, exc);
                }
                if (value.canBeNonNull()) {
                    successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                    value = (AValue) successorFrame.popOperand();

                    FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
                    Type fieldType = Type.getType(fieldInsnNode.desc);
                    String fieldName = fieldInsnNode.owner.replace('/', '.') +
                            '.' + fieldInsnNode.name;
                    resultValue = newValue(fieldType, false);

                    if (resultValue instanceof ObjectValue) {
                        ModelableAValue mavalue = (ModelableAValue) resultValue;
                        String mavalueName = getModelNameAtNode(this.activeNode,
                                mavalue.getTypeName());
                        Model model = getModel(mavalueName);
                        mavalue.enableModeling(model, mavalueName);
                        mavalue.fieldValue(fieldName, fieldInsnNode.desc);
                    }

                    successorFrame.pushOperand(resultValue);
                }
                break;

            case Opcodes.GETSTATIC:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();

                FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
                Type fieldType = Type.getType(fieldInsnNode.desc);
                String fieldName = fieldInsnNode.owner.replace('/', '.') + '.' +
                        fieldInsnNode.name;
                resultValue = newValue(fieldType, false);

                if (resultValue instanceof ObjectValue) {
                    ModelableAValue mavalue = (ModelableAValue) resultValue;
                    String mavalueName = getModelNameAtNode(this.activeNode,
                            mavalue.getTypeName());
                    Model model = getModel(mavalueName);
                    mavalue.enableModeling(model, mavalueName);
                    mavalue.fieldValue(fieldName, fieldInsnNode.desc);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2D:
            case Opcodes.I2F:
            case Opcodes.I2L:
            case Opcodes.I2S:
            case Opcodes.INEG:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();

                Set<IntegerValue> integerValues = value.getIntegerValues();
                Iterator<IntegerValue> integerValuesIterator =
                        integerValues.iterator();
                resultValue = integerValuesIterator.next().
                        performOperation(node.getOpcode());
                merges = new MergesCache();
                while (integerValuesIterator.hasNext()) {
                    IntegerValue integer = integerValuesIterator.next();
                    resultValue = integer.performOperation(node.getOpcode()).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.IADD:
            case Opcodes.IAND:
            case Opcodes.IMUL:
            case Opcodes.IOR:
            case Opcodes.IXOR:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                integerValues = value1.getIntegerValues();
                integerValuesIterator = integerValues.iterator();
                IntegerValue integer = integerValuesIterator.next();
                resultValue = integer.performCommutativeOperation(node.getOpcode(), value2);
                merges = new MergesCache();
                while (integerValuesIterator.hasNext()) {
                    integer = integerValuesIterator.next();
                    resultValue = integer.performCommutativeOperation(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                integerValues = value2.getIntegerValues();
                integerValuesIterator = integerValues.iterator();
                integer = integerValuesIterator.next();
                resultValue = integer.performCommutativeOperation(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (integerValuesIterator.hasNext()) {
                    integer = integerValuesIterator.next();
                    resultValue = integer.performCommutativeOperation(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.ISUB:
            case Opcodes.IUSHR:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                integerValues = value1.getIntegerValues();
                integerValuesIterator = integerValues.iterator();
                integer = integerValuesIterator.next();
                resultValue = integer.performNonCommutativeOperationLeft(node.getOpcode(), value2);
                merges = new MergesCache();
                while (integerValuesIterator.hasNext()) {
                    integer = integerValuesIterator.next();
                    resultValue = integer.performNonCommutativeOperationLeft(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                integerValues = value2.getIntegerValues();
                integerValuesIterator = integerValues.iterator();
                integer = integerValuesIterator.next();
                resultValue = integer.performNonCommutativeOperationRight(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (integerValuesIterator.hasNext()) {
                    integer = integerValuesIterator.next();
                    resultValue = integer.performNonCommutativeOperationRight(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
                Frame trueSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getTrueSuccessorFrame();
                value2 = (AValue) trueSuccessorFrame.popOperand();
                value1 = (AValue) trueSuccessorFrame.popOperand();

                Frame falseSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getFalseSuccessorFrame();
                value2 = (AValue) falseSuccessorFrame.popOperand();
                value1 = (AValue) falseSuccessorFrame.popOperand();
                break;

            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
                trueSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getTrueSuccessorFrame();
                value = (AValue) trueSuccessorFrame.popOperand();
                falseSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getFalseSuccessorFrame();
                value = (AValue) falseSuccessorFrame.popOperand();
                break;

            case Opcodes.IFNONNULL:
                frame = this.activeNode.getIn().copy();
                value = (AValue) frame.popOperand();

                trueSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getTrueSuccessorFrame();
                value = (AValue) trueSuccessorFrame.popOperand();

                falseSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getFalseSuccessorFrame();
                value = (AValue) falseSuccessorFrame.popOperand();
                break;

            case Opcodes.IFNULL:
                frame = this.activeNode.getIn().copy();
                value = (AValue) frame.popOperand();

                trueSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getTrueSuccessorFrame();
                value = (AValue) trueSuccessorFrame.popOperand();

                falseSuccessorFrame =
                        ((ComparisonNode) this.activeNode).getFalseSuccessorFrame();
                value = (AValue) falseSuccessorFrame.popOperand();
                break;

            case Opcodes.IINC:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();

                IincInsnNode iincInsnNode = (IincInsnNode) node;
                value = (AValue) successorFrame.getLocalVariable(iincInsnNode.var);
                int increment = iincInsnNode.incr;
                integerValues = value.getIntegerValues();
                integerValuesIterator = integerValues.iterator();
                resultValue = integerValuesIterator.next().
                        performIincOperation(increment);
                merges = new MergesCache();
                while (integerValuesIterator.hasNext()) {
                    integer = integerValuesIterator.next();
                    resultValue = integer.performIincOperation(increment).
                            merge(resultValue, merges);
                }

                successorFrame.setLocalVariable(iincInsnNode.var, resultValue);
                break;

            case Opcodes.INSTANCEOF:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();
                resultValue = IntegerValue.getInstance();
                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEVIRTUAL:
                // prepare the method data
                MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                String typeName = methodInsnNode.owner.replace('/', '.');
                String methodName = methodInsnNode.name + methodInsnNode.desc;
                int paramsNum = Type.getArgumentTypes(methodInsnNode.desc).length;
                AValue objectref;
                AValue returnValue = null;
                JavaMethod method = null;

                // deal with target object being possibly null
                frame = this.activeNode.getIn().copy();
                values.clear();
                for (int i = 0; i < paramsNum + 1; i++) {
                    values.add(0, (AValue) frame.popOperand());
                }
                objectref = values.get(0);
                if (objectref.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    ((SimpleNode) this.activeNode).addException(excType, exc);
                }

                // deal with target object being possibly non-null
                if (objectref.canBeNonNull()) {
                    // get the most specific name of the type being the target
                    // of the call
                    JavaClassPool classPool = JavaClassPool.get();
                    String targetTypeName = typeName;

                    if (!typeName.startsWith("[")) {
                        // take into account all exceptions that may get thrown
                        try {
                            JavaType targetType =
                                    classPool.getType(targetTypeName, false);

                            // make sure the method exists in the type calculated
                            try {
                                targetType.getMethod(methodName);
                            } catch (NoSuchMethodException e) {
                                System.err.println("[ERROR] This should never happen");
                                e.printStackTrace(System.err);
                                throw new InternalError();
                            }

                            method = targetType.getMethod(methodName);
                            for (String excType : method.getExceptions()) {
                                Frame exceptionFrame = ((SimpleNode) this.activeNode).
                                        getExceptionFrame(excType);

                                // get exception, target object and all arguments
                                Value frameExc = exceptionFrame.popOperand();
                                values.clear();
                                for (int i = 0; i < paramsNum + 1; i++) {
                                    values.add(0, (AValue) exceptionFrame.popOperand());
                                }

                                // put the frame in the proper state
                                for (int i = 0; i < paramsNum + 1; i++) {
                                    exceptionFrame.pushOperand(values.get(i));
                                }
                                exceptionFrame.pushOperand(frameExc);

                                // get mapping from values to parameters' sets
                                IdentityHashMap<ModelableAValue, List<Integer>> arg2params =
                                        new IdentityHashMap<ModelableAValue, List<Integer>>();
                                for (int i = 0; i < values.size(); i++) {
                                    Set<ModelableAValue> mavalues =
                                            values.get(i).getModelableRepresentations();
                                    for (ModelableAValue mavalue : mavalues) {
                                        if (!arg2params.containsKey(mavalue)) {
                                            arg2params.put(mavalue, new ArrayList<Integer>());
                                        }
                                        arg2params.get(mavalue).add(i);
                                    }
                                }

                                // notify arguments about possible exception
                                for (ModelableAValue mavalue : arg2params.keySet()) {
                                    mavalue.invokeMethodException(targetTypeName,
                                            methodName, arg2params.get(mavalue),
                                            excType);
                                }

                                ObjectValue exc = newObjectValue(excType, false);
                                ((SimpleNode) this.activeNode).addException(excType,
                                        exc);
                            }
                        } catch (ClassNotFoundException e) {
                            // ignore the error; this makes the analysis more
                            // imprecise, but is more robust than bailing out
                        } catch (NoSuchMethodException e) {
                            System.err.println("[ERROR] This should never happen");
                            e.printStackTrace(System.err);
                            throw new InternalError();
                        }
                    }

                    successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();

                    // get target object and all arguments
                    values.clear();
                    for (int i = 0; i < paramsNum + 1; i++) {
                        values.add(0, (AValue) successorFrame.popOperand());
                    }
                    objectref = values.get(0);

                    // prepare return value if possible
                    if (method != null && method.alwaysReturnsTheSameVariable()) {
                        if (method.alwaysReturnsThis()) {
                            returnValue = objectref;
                        } else {
                            for (int i = 1; i <= paramsNum; i++) {
                                if (method.alwaysReturnsParameter(i)) {
                                    returnValue = values.get(i);
                                }
                            }
                        }

                        if (returnValue == null) {
                            throw new InternalError();
                        }
                    }

                    // get mapping from values to parameters' sets
                    IdentityHashMap<ModelableAValue, List<Integer>> arg2params =
                            new IdentityHashMap<ModelableAValue, List<Integer>>();
                    for (int i = 0; i < values.size(); i++) {
                        Set<ModelableAValue> mavalues =
                                values.get(i).getModelableRepresentations();
                        for (ModelableAValue mavalue : mavalues) {
                            if (!arg2params.containsKey(mavalue)) {
                                arg2params.put(mavalue, new ArrayList<Integer>());
                            }
                            arg2params.get(mavalue).add(i);
                        }
                    }

                    // add call to this method to models of parameters
                    for (ModelableAValue mavalue : arg2params.keySet()) {
                        mavalue.invokeMethod(targetTypeName, methodName,
                                arg2params.get(mavalue));
                    }

                    // create the return value
                    Type returnType = Type.getReturnType(methodInsnNode.desc);
                    if (returnType.getSort() != Type.VOID) {
                        if (returnValue == null) {
                            returnValue = newValue(returnType, false);
                            if (returnValue instanceof ObjectValue) {
                                ModelableAValue mavalue = (ModelableAValue) returnValue;
                                String mavalueName = getModelNameAtNode(this.activeNode,
                                        mavalue.getTypeName());
                                Model model = getModel(mavalueName);
                                mavalue.enableModeling(model, mavalueName);
                                if (!targetTypeName.startsWith("[")) {
                                    mavalue.returnValueOfMethod(targetTypeName,
                                            methodName);
                                }
                            }
                        }
                        successorFrame.pushOperand(returnValue);
                    }
                }
                break;

            case Opcodes.INVOKESTATIC:
                // prepare the method data
                methodInsnNode = (MethodInsnNode) node;
                String className = methodInsnNode.owner.replace('/', '.');
                methodName = methodInsnNode.name + methodInsnNode.desc;
                paramsNum = Type.getArgumentTypes(methodInsnNode.desc).length;
                List<AValue> args = new ArrayList<AValue>();
                returnValue = null;
                method = null;

                // take into account all exceptions that may get thrown
                try {
                    JavaClass clas =
                            (JavaClass) JavaClassPool.get().getType(className, false);
                    method = clas.getMethod(methodName);
                    for (String excType : method.getExceptions()) {
                        Frame exceptionFrame = ((SimpleNode) this.activeNode).
                                getExceptionFrame(excType);

                        // get exception and all arguments
                        Value frameExc = exceptionFrame.popOperand();
                        args.clear();
                        for (int i = 0; i < paramsNum; i++) {
                            args.add(0, (AValue) exceptionFrame.popOperand());
                        }

                        // put the frame in the proper state
                        for (int i = 0; i < paramsNum; i++) {
                            exceptionFrame.pushOperand(args.get(i));
                        }
                        exceptionFrame.pushOperand(frameExc);

                        // get mapping from values to parameters' sets
                        IdentityHashMap<ModelableAValue, List<Integer>> arg2params =
                                new IdentityHashMap<ModelableAValue, List<Integer>>();
                        for (int i = 0; i < args.size(); i++) {
                            Set<ModelableAValue> mavalues =
                                    args.get(i).getModelableRepresentations();
                            for (ModelableAValue mavalue : mavalues) {
                                if (!arg2params.containsKey(mavalue)) {
                                    arg2params.put(mavalue, new ArrayList<Integer>());
                                }
                                arg2params.get(mavalue).add(i + 1);
                            }
                        }

                        // notify arguments about possible exception
                        for (ModelableAValue mavalue : arg2params.keySet()) {
                            mavalue.invokeMethodException(className,
                                    methodName, arg2params.get(mavalue), excType);
                        }

                        ObjectValue exc = newObjectValue(excType, false);
                        ((SimpleNode) this.activeNode).addException(excType, exc);
                    }
                } catch (ClassNotFoundException e) {
                    // ignore the error; this makes the analysis more
                    // imprecise, but is more robust than bailing out
                } catch (NoSuchMethodException e) {
                    System.err.println("[ERROR] This should never happen");
                    e.printStackTrace(System.err);
                    throw new InternalError();
                }

                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();

                // get target object and all arguments
                args.clear();
                for (int i = 0; i < paramsNum; i++) {
                    args.add(0, (AValue) successorFrame.popOperand());
                }

                // prepare return value if possible
                if (method != null && method.alwaysReturnsTheSameVariable()) {
                    if (method.alwaysReturnsThis()) {
                        throw new InternalError();
                    } else {
                        for (int i = 1; i <= paramsNum; i++) {
                            if (method.alwaysReturnsParameter(i)) {
                                returnValue = args.get(i - 1);
                            }
                        }
                    }

                    if (returnValue == null) {
                        throw new InternalError();
                    }
                }

                // get mapping from values to parameters' sets
                IdentityHashMap<ModelableAValue, List<Integer>> arg2params =
                        new IdentityHashMap<ModelableAValue, List<Integer>>();
                for (int i = 0; i < args.size(); i++) {
                    Set<ModelableAValue> mavalues =
                            args.get(i).getModelableRepresentations();
                    for (ModelableAValue mavalue : mavalues) {
                        if (!arg2params.containsKey(mavalue)) {
                            arg2params.put(mavalue, new ArrayList<Integer>());
                        }
                        arg2params.get(mavalue).add(i + 1);
                    }
                }

                // add call to this method to models of parameters
                for (ModelableAValue mavalue : arg2params.keySet()) {
                    mavalue.invokeMethod(className, methodName,
                            arg2params.get(mavalue));
                }

                // create the return value
                Type returnType = Type.getReturnType(methodInsnNode.desc);
                if (returnType.getSort() != Type.VOID) {
                    if (returnValue == null) {
                        returnValue = newValue(returnType, false);
                        if (returnValue instanceof ObjectValue) {
                            ModelableAValue mavalue = (ModelableAValue) returnValue;
                            String mavalueName = getModelNameAtNode(this.activeNode,
                                    mavalue.getTypeName());
                            Model model = getModel(mavalueName);
                            mavalue.enableModeling(model, mavalueName);
                            if (!className.startsWith("[")) {
                                mavalue.returnValueOfMethod(className,
                                        methodName);
                            }
                        }
                    }
                    successorFrame.pushOperand(returnValue);
                }
                break;

            case Opcodes.L2D:
            case Opcodes.L2F:
            case Opcodes.L2I:
            case Opcodes.LNEG:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();

                Set<LongValue> longValues = value.getLongValues();
                Iterator<LongValue> longValuesIterator = longValues.iterator();
                resultValue = longValuesIterator.next().
                        performOperation(node.getOpcode());
                merges = new MergesCache();
                while (longValuesIterator.hasNext()) {
                    LongValue long_ = longValuesIterator.next();
                    resultValue = long_.performOperation(node.getOpcode()).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.LADD:
            case Opcodes.LAND:
            case Opcodes.LMUL:
            case Opcodes.LOR:
            case Opcodes.LXOR:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                longValues = value1.getLongValues();
                longValuesIterator = longValues.iterator();
                LongValue long_ = longValuesIterator.next();
                resultValue = long_.performCommutativeOperation(node.getOpcode(), value2);
                merges = new MergesCache();
                while (longValuesIterator.hasNext()) {
                    long_ = longValuesIterator.next();
                    resultValue = long_.performCommutativeOperation(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                longValues = value2.getLongValues();
                longValuesIterator = longValues.iterator();
                long_ = longValuesIterator.next();
                resultValue = long_.performCommutativeOperation(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (longValuesIterator.hasNext()) {
                    long_ = longValuesIterator.next();
                    resultValue = long_.performCommutativeOperation(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.LCMP:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LSUB:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                longValues = value1.getLongValues();
                longValuesIterator = longValues.iterator();
                long_ = longValuesIterator.next();
                resultValue = long_.performNonCommutativeOperationLeft(node.getOpcode(), value2);
                merges = new MergesCache();
                while (longValuesIterator.hasNext()) {
                    long_ = longValuesIterator.next();
                    resultValue = long_.performNonCommutativeOperationLeft(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                longValues = value2.getLongValues();
                longValuesIterator = longValues.iterator();
                long_ = longValuesIterator.next();
                resultValue = long_.performNonCommutativeOperationRight(node.getOpcode(), value1).
                        merge(resultValue, merges);
                while (longValuesIterator.hasNext()) {
                    long_ = longValuesIterator.next();
                    resultValue = long_.performNonCommutativeOperationRight(node.getOpcode(), value1).
                            merge(resultValue, merges);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value2 = (AValue) successorFrame.popOperand();
                value1 = (AValue) successorFrame.popOperand();

                // perform operation on the left-hand side arguments
                longValues = value1.getLongValues();
                longValuesIterator = longValues.iterator();
                long_ = longValuesIterator.next();
                resultValue = long_.performNonCommutativeOperationLeft(node.getOpcode(), value2);
                merges = new MergesCache();
                while (longValuesIterator.hasNext()) {
                    long_ = longValuesIterator.next();
                    resultValue = long_.performNonCommutativeOperationLeft(node.getOpcode(), value2).
                            merge(resultValue, merges);
                }

                // perform operation on the right-hand side arguments
                integerValues = value2.getIntegerValues();
                integerValuesIterator = integerValues.iterator();
                while (integerValuesIterator.hasNext()) {
                    integer = integerValuesIterator.next();
                    integer.performNonCommutativeOperationRight(node.getOpcode(), value1);
                }

                successorFrame.pushOperand(resultValue);
                break;

            case Opcodes.LDC:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();

                LdcInsnNode ldcInsnNode = (LdcInsnNode) node;
                if (ldcInsnNode.cst instanceof Integer) {
                    Integer initializer = (Integer) ldcInsnNode.cst;
                    value = newIntegerValue(initializer);
                } else if (ldcInsnNode.cst instanceof Float) {
                    Float initializer = (Float) ldcInsnNode.cst;
                    value = newFloatValue(initializer);
                } else if (ldcInsnNode.cst instanceof Long) {
                    Long initializer = (Long) ldcInsnNode.cst;
                    value = newLongValue(initializer);
                } else if (ldcInsnNode.cst instanceof Double) {
                    Double initializer = (Double) ldcInsnNode.cst;
                    value = newDoubleValue(initializer);
                } else if (ldcInsnNode.cst instanceof String) {
                    className = "java.lang.String";
                    value = newObjectValue(className, true);
                } else {
                    value = newValue((Type) ldcInsnNode.cst, true);
                }
                if (value instanceof ObjectValue) {
                    ModelableAValue mavalue = (ModelableAValue) value;
                    String mavalueName = getModelNameAtNode(this.activeNode,
                            mavalue.getTypeName());
                    Model model = getModel(mavalueName);
                    mavalue.enableModeling(model, mavalueName);
                }

                successorFrame.pushOperand(value);
                break;

            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                frame = this.activeNode.getIn().copy();
                value = (AValue) frame.popOperand();

                if (value.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    ((SimpleNode) this.activeNode).addException(excType, exc);
                }
                if (value.canBeNonNull()) {
                    // we don't care about synchronization
                    successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                    successorFrame.popOperand();
                }
                break;

            case Opcodes.MULTIANEWARRAY:
                MultiANewArrayInsnNode multiANewArrayInsnNode =
                        (MultiANewArrayInsnNode) node;
                int dimensions = multiANewArrayInsnNode.dims;
                descriptor = multiANewArrayInsnNode.desc;

                // pop dimensions from the stack in the reverse order
                AValue[] dimValues = new AValue[dimensions];
                successorFrame =
                        ((SimpleNode) this.activeNode).getSuccessorFrame();
                for (int i = 0; i < dimensions; i++) {
                    dimValues[i] = (AValue) successorFrame.popOperand();
                }

                // determine the initialized element of the array
                Type arrayType = Type.getType(descriptor);
                if (dimensions == arrayType.getDimensions()) {
                    element = newValue(arrayType.getElementType(), false);
                } else {
                    element = newNullValue();
                }

                // create the array
                arrayValue = element;
                for (int i = 0; i < dimensions; i++) {
                    arrayValue = new ArrayValue((AValue) arrayValue, dimValues[i]);
                }

                successorFrame.pushOperand(arrayValue);
                break;

            case Opcodes.NEW:
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();

                typeInsnNode = (TypeInsnNode) node;
                className = typeInsnNode.desc.replace('/', '.');
                ObjectValue objectValue = newObjectValue(className, true);
                String objectValueName = getModelNameAtNode(this.activeNode, className);
                Model model = getModel(objectValueName);
                objectValue.enableModeling(model, objectValueName);

                successorFrame.pushOperand(objectValue);
                break;

            case Opcodes.NEWARRAY:
                IntInsnNode intInsnNode = (IntInsnNode) node;
                switch (intInsnNode.operand) {
                    case 4:
                    case 5:
                    case 8:
                    case 9:
                    case 10:
                        element = newIntegerValue(0);
                        break;

                    case 6:
                        element = newFloatValue(0.0f);
                        break;

                    case 7:
                        element = newDoubleValue(0.0);
                        break;

                    case 11:
                        element = newLongValue(0L);
                        break;

                    default:
                        System.err.println("[ERROR] This should never happen");
                        throw new IllegalStateException("operand = " + intInsnNode.operand);
                }

                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();

                arrayValue = new ArrayValue(element, value);

                successorFrame.pushOperand(arrayValue);
                break;

            case Opcodes.LOOKUPSWITCH:
                Frame defaultSuccessorFrame =
                        ((SwitchNode) this.activeNode).getDefaultSuccessorFrame();
                value = (AValue) defaultSuccessorFrame.popOperand();
                LookupSwitchInsnNode lookupSwitchInsnNode =
                        (LookupSwitchInsnNode) node;
                for (Object keyObject : lookupSwitchInsnNode.keys) {
                    Integer key = (Integer) keyObject;
                    Frame keySuccessorFrame =
                            ((SwitchNode) this.activeNode).getKeySuccessorFrame(key);
                    value = (AValue) keySuccessorFrame.popOperand();
                }
                break;

            case Opcodes.PUTFIELD:
                frame = this.activeNode.getIn().copy();
                value2 = (AValue) frame.popOperand();
                value1 = (AValue) frame.popOperand();

                fieldInsnNode = (FieldInsnNode) node;
                if (value1.canBeNull()) {
                    String excType = NullPointerException.class.getName();
                    ObjectValue exc = newObjectValue(excType, true);
                    ((SimpleNode) this.activeNode).addException(excType, exc);
                }
                if (value1.canBeNonNull()) {
                    successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                    value2 = (AValue) successorFrame.popOperand();
                    value1 = (AValue) successorFrame.popOperand();
                }
                break;

            case Opcodes.PUTSTATIC:
                // don't do anything
                successorFrame = ((SimpleNode) this.activeNode).getSuccessorFrame();
                value = (AValue) successorFrame.popOperand();
                break;

            case Opcodes.TABLESWITCH:
                defaultSuccessorFrame =
                        ((SwitchNode) this.activeNode).getDefaultSuccessorFrame();
                value = (AValue) defaultSuccessorFrame.popOperand();
                TableSwitchInsnNode tableSwitchInsnNode =
                        (TableSwitchInsnNode) node;
                for (int i = tableSwitchInsnNode.min; i <= tableSwitchInsnNode.max; i++) {
                    Frame keySuccessorFrame =
                            ((SwitchNode) this.activeNode).getKeySuccessorFrame(i);
                    value = (AValue) keySuccessorFrame.popOperand();
                }
                break;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " + node.getOpcode());
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#newNullValue()
     */
    @Override
    public NullValue newNullValue() {
        return this.nullValue;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#newIntegerValue(int)
     */
    @Override
    public IntegerValue newIntegerValue(int value) {
        return IntegerValue.getInstance();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#newLongValue(long)
     */
    @Override
    public LongValue newLongValue(long value) {
        return LongValue.getInstance();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#newDoubleValue(double)
     */
    @Override
    public DoubleValue newDoubleValue(double value) {
        return DoubleValue.getInstance();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#newFloatValue(float)
     */
    @Override
    public FloatValue newFloatValue(float value) {
        return FloatValue.getInstance();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#newObjectValue(java.lang.String, boolean)
     */
    @Override
    public ObjectValue newObjectValue(String typeName, boolean exactType) {
        return new ObjectValue(typeName, exactType);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Analyzer#getExactValues(org.softevo.oumextractor.analysis.Value)
     */
    @Override
    public Map<Value, Boolean> getExactValues(Value in_value) {
        AValue value = (AValue) in_value;
        Map<Value, Boolean> result = new HashMap<Value, Boolean>();

        for (ObjectValue objectValue : value.getObjectValues()) {
            result.put(objectValue, objectValue.isTypeExact());
        }

        return result;
    }

    /**
     * Creates and returns new value of given type.
     *
     * @param type      Type of the value to create.
     * @param exactType If <code>true</code>, any object value created in this
     *                  method will represent exactly the type it is supposed
     *                  to; otherwise it will represent additionally all
     *                  possible subtypes of the type.
     * @return New value of given type.
     */
    private AValue newValue(Type type, boolean exactType) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return IntegerValue.getInstance();

            case Type.FLOAT:
                return FloatValue.getInstance();

            case Type.LONG:
                return LongValue.getInstance();

            case Type.DOUBLE:
                return DoubleValue.getInstance();

            case Type.ARRAY:
                AValue element = newValue(type.getElementType(), exactType);
                for (int i = 1; i < type.getDimensions(); i++) {
                    element = new ArrayValue(element, IntegerValue.getInstance());
                }
                return new ArrayValue(element, IntegerValue.getInstance());

            case Type.OBJECT:
                return newObjectValue(type.getClassName(), exactType);

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalArgumentException("type sort = " + type.getSort());
        }
    }

    /**
     * Adds model of a given name to the list of models used.
     *
     * @param name  Name of the model.
     * @param model Model to be added.
     */
    private void addModel(String name, Model model) {
        System.out.println("MODEL NAME " + name);
        this.currentModels.put(name, model);
        this.modelsToSerialize.add(model);
    }

    /**
     * Gets model of a given name.  If such a model does not exist, an empty model
     * is created and put into the view of currently active models under given name.
     *
     * @param name Name of the model to retrieve/create.
     * @return Model for object of given name.
     */
    private Model getModel(String name) {
        if (!this.currentModels.containsKey(name)) {
            Model model = new Model();
            addModel(name, model);
        }
        return this.currentModels.get(name);
    }

    /**
     * Gets model name for a variable of given type created at given node.
     *
     * @param node     Node where the object is created.
     * @param typeName Fully qualified name of a type of created object.
     * @return Model name for an object created at given node.
     */
    private String getModelNameAtNode(Node node, String typeName) {
        if (!this.nodeModels.containsKey(node)) {
            String modelName = "var #" + (this.nodeModels.size() + 1) +
                    "@" + typeName;
            if (node instanceof BytecodeNode) {
                BytecodeNode bytecodeNode = (BytecodeNode) node;
                modelName = modelName + " (line " +
                        bytecodeNode.getLineNumber() + ")";
            }
            this.nodeModels.put(node, modelName);
        }
        return this.nodeModels.get(node);
    }

    /**
     * Serializes id2modelData field of the analyzer into given stream.
     *
     * @param out Stream to serialize the field to.
     * @throws IOException
     */
    private void writeId2ModelData(ObjectOutputStream out) throws IOException {
        out.writeInt(this.id2modelData.size());
        for (Integer id : this.id2modelData.keySet()) {
            ModelData modelData = this.id2modelData.get(id);
            out.writeInt(id);
            out.writeObject(modelData);
        }
    }

    /**
     * Serializes list of types analyzed by this Analyzer into given stream.
     *
     * @param out Stream to serialize the types' names to.
     * @throws IOException
     */
    private void writeTypesNames(ObjectOutputStream out) throws IOException {
        out.writeInt(this.typesNames.size());
        for (String typeName : this.typesNames) {
            out.writeObject(typeName);
        }
    }
}
