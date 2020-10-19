package org.softevo.oumextractor.modelcreator1;

import java.io.Serializable;

/**
 * This class is used to represent data about a model.
 *
 * @author Andrzej Wasylkowski
 */
final public class ModelData implements Serializable {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -7395508096322161590L;

    /**
     * Fully qualified name of the class, analysis of which produced the model.
     */
    private final String className;

    /**
     * Name and signature of the method, analysis of which produced the model.
     */
    private final String methodName;

    /**
     * Name of the model.
     */
    private final String modelName;

    /**
     * Creates new structure of model data.
     *
     * @param className  Fully qualified name of the class, analysis of
     *                   which produced the model.
     * @param methodName Name and signature of the method, analysis of which
     *                   produced the model.
     * @param modelName  Name of the model.
     */
    public ModelData(String className, String methodName, String modelName) {
        this.className = className;
        this.methodName = methodName;
        this.modelName = modelName;
    }

    /**
     * Returns class, analysis of which produced the model.
     *
     * @return Fully qualified name of the class.
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Returns method, analysis of which produced the model.
     *
     * @return Name and signature of the method.
     */
    public String getMethodName() {
        return this.methodName;
    }

    /**
     * Returns name of the model.
     *
     * @return Name of the model.
     */
    public String getModelName() {
        return this.modelName;
    }
}
