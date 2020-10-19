package org.softevo.oumextractor.modelcreator1;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.softevo.oumextractor.analysis.Value;
import org.softevo.oumextractor.controlflow.Node;
import org.softevo.oumextractor.modelcreator1.model.*;

import java.util.List;

/**
 * This is the base class for representing modelable values. To make a value
 * a modelable value you have to do the following things:
 * 1) inherit from ModelableAValue
 * 2) in you equals method call the modelwiseEquals method from the
 * ModelableAValue, so that the models are taken into account when comparing
 * 3) in your hashCode method, include the hashCode of the ModelableAValue
 * 4) in your merge method use the result of the modelwiseMerge method from
 * ModelableAValue as the merge result if the two values could otherwise be
 * merged into one (i.e. are equivalent)
 * 5) in your clone method call the postClone method using the cloned value
 * as a parameter immediately after creating the clone (and so before using
 * it in any other way)
 * 6) you may choose to override (i.e. add additional behavior; remember to
 * call the overriden method in any event) the reenableModeling method
 * 7) by default, methods initialValue, fieldValue, staticFieldValue,
 * invokeExternalMethod, invokeExternalMethodException,
 * invokeExternalStaticMethod, invokeExternalStaticMethodException,
 * returnFromMethod, returnValueOfMethod and unhandledException are provided
 * 8) if you want to add new transition-generating methods, call the
 * performTransition method to actually cause the transition you have created
 * to happen. Remember that modeling has to be enabled for this method to
 * work, so don't forget to check if it is in your method.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class ModelableAValue extends AValue {

    /**
     * Name of the type (either primitive name or fully qualified class name)
     * of the modeled value.
     */
    protected String typeName;

    /**
     * Model of this value or <code>null</code> if value is not being modeled.
     */
    protected Model model;

    /**
     * Indicator, whether this value is being modeled or not.
     */
    protected boolean modelingEnabled;

    /**
     * State of this value.  Must be <code>null</code> if not modeling.
     */
    protected State state;

    /**
     * Internal name of a variable that this value models or empty string if
     * value is not being modeled.
     */
    protected String varName;

    /**
     * Initializes the value to represent a value that is not being modeled.
     *
     * @param typeName Name of the type of the modeled value.
     */
    protected ModelableAValue(String typeName) {
        this.typeName = typeName;
        this.model = null;
        this.modelingEnabled = false;
        this.state = null;
        this.varName = "";
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#hashCode()
     */
    @Override
    public int hashCode() {
        // the hash code is selected so that it will not change during the
        // lifetime of the object
        return new HashCodeBuilder(21, 37).
                toHashCode();
    }

    /**
     * This method has to be called after cloning so that the cloned value can
     * be updated.
     *
     * @param clone Clone of this.
     */
    public void postClone(ModelableAValue clone) {
        clone.state = this.state;
    }

    /**
     * Enables modeling of this value.
     *
     * @param model   Model to be used.
     * @param varName Internal name of a variable this value represents.
     */
    public final void enableModeling(Model model, String varName) {
        if (this instanceof ObjectValue) {
            this.model = model;
            this.modelingEnabled = true;
            this.varName = varName;
            this.state = model.getEntryState();
        }
    }

    /**
     * Disables modeling of this value.
     */
    public final void disableModeling() {
        this.model = null;
        this.modelingEnabled = false;
        this.varName = "";
        this.state = null;
    }

    /**
     * Returns name of the type of the represented object.
     *
     * @return Name of the type of the represented object.
     */
    public final String getTypeName() {
        return this.typeName;
    }

    /**
     * Indicates, whether this value is being modeled or not.
     *
     * @return <code>true</code> if modeling of this value is enabled;
     * <code>false</code> otherwise.
     */
    public final boolean modelingEnabled() {
        return this.modelingEnabled;
    }

    /**
     * Returns model being used for this value.
     *
     * @return Model being used for this value.
     */
    public final Model getModel() {
        return this.model;
    }

    /**
     * Called to inform this value that given node has just been analyzed.
     *
     * @param node Node that has just been analyzed.
     */
    public final void setCurrentNode(Node node) {
        if (this.modelingEnabled) {
            State nodeState = model.getStateForNode(node);
            if (nodeState != this.state) {
                this.model.addTransition(this.state, nodeState,
                        EpsilonTransition.get());
            }
            this.state = nodeState;
        }
    }

    /**
     * Performs a given transition on the model of this value.
     *
     * @param transition Transition to perform on the model of this value.
     */
    public final void performTransition(Transition transition) {
        if (!this.modelingEnabled) {
            throw new IllegalStateException();
        }
        State to = this.model.getFollowUpState(this.state, transition);
        this.model.addTransition(this.state, to, transition);
        this.state = to;
    }

    /**
     * Called to inform this object that it was taken from the given field.
     *
     * @param fieldName Full name of the field the value came from.
     * @param typeName  Internal name of the type of the field.
     */
    public final void fieldValue(String fieldName, String typeName) {
        if (modelingEnabled()) {
            Transition transition = FieldValueTransition.get(fieldName,
                    typeName);
            performTransition(transition);
        }
    }

    /**
     * Called to inform this value that it was cast to a given type.
     *
     * @param typeName Fully qualified name of a type, to which this
     *                 value was cast.
     */
    public final void castToType(String typeName) {
        if (this.modelingEnabled) {
            Transition transition = CastTransition.get(typeName);
            performTransition(transition);
        }
    }

    /**
     * Called to inform this value that a method of given name from given type
     * is about to be invoked with this value as a parameter at a given
     * position.
     *
     * @param typeName         Fully qualified name of a type, on which a
     *                         method will be called.
     * @param methodName       Method name (with its descriptor).
     * @param parameterIndices Indices of this value as parameters of a
     *                         called method.
     */
    public final void invokeMethod(String typeName, String methodName,
                                   List<Integer> parameterIndices) {
        if (this.modelingEnabled) {
            MethodCall methodCall = new MethodCall(typeName, methodName);
            Transition transition =
                    InvokeMethodTransition.get(methodCall, parameterIndices);
            performTransition(transition);
        }
    }

    /**
     * Called to inform this value that an invocation of a method of given name
     * from given type with this value as a parameter at a given position ends
     * with an exception of given type.
     *
     * @param typeName         Fully qualified name of a type, on which a
     *                         method will be called.
     * @param methodName       Method name (with its descriptor).
     * @param parameterIndices Indices of this value as parameters of a
     *                         called method.
     * @param excType          Fully qualified name of an exception class.
     */
    public final void invokeMethodException(String typeName, String methodName,
                                            List<Integer> parameterIndices, String excType) {
        if (this.modelingEnabled) {
            MethodCall methodCall = new MethodCall(typeName, methodName);
            Transition transition = ExceptionTransition.get(
                    InvokeMethodTransition.get(methodCall, parameterIndices),
                    excType);
            performTransition(transition);
        }
    }

    /**
     * Called to inform this value that it is a return value of a given method.
     *
     * @param typeName Fully qualified name of a type declaring
     *                 called method.
     * @param name     Method name (with its descriptor).
     */
    public final void returnValueOfMethod(String typeName, String name) {
        if (this.modelingEnabled) {
            MethodCall methodCall = new MethodCall(typeName, name);
            Transition transition =
                    ReturnValueOfMethodTransition.get(methodCall);
            performTransition(transition);
        }
    }

    /**
     * Called to inform this value that a return from method was encountered.
     *
     * @param value Return value or <code>null</code> in case of
     *              <code>void</code>.
     */
    public final void returnFromMethod(Value value) {
        if (this.modelingEnabled) {
            State to = this.model.getExitState();
            if (this.state != to) {
                this.model.addTransition(this.state, to, EpsilonTransition.get());
            }
            this.state = to;
        }
    }

    /**
     * Called to inform this value that an unhandled exception was encountered.
     *
     * @param excType Fully qualified name of an exception class.
     */
    public final void unhandledException(String excType) {
        if (this.modelingEnabled) {
            State to = this.model.getAbnormalExitState(excType);
            State from = this.state;
            Transition transition = EpsilonTransition.get();
            if (from != to) {
                this.model.addTransition(from, to, transition);
            }
            this.state = to;
        }
    }

    /**
     * Returns <code>true</code> if this value is modelwise equal to the given
     * value.  "Modelwise" means that either both of them are not modeled or
     * both of them are modeled and have identical models in the same state.
     *
     * @param other Value to compare this value with.
     * @return <code>true</code> if this value is modelwise equal to the given
     * value; <code>false</code> if it is not.
     */
    protected boolean modelwiseEquals(ModelableAValue other) {
        if (this == other) {
            return true;
        }
        if (this.typeName.equals(other.typeName) &&
                this.modelingEnabled == other.modelingEnabled &&
                this.varName.equals(other.varName)) {
            if (this.modelingEnabled) {
                if (this.model != other.model) {
                    throw new IllegalStateException();
                } else if (this.state == other.state) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Performs a modelwise merge on this value and the given value. "Modelwise"
     * means that only models of both values are taken into account. This
     * method is intended to be used by "merge" method after it has decided that
     * two values can be merged on other grounds
     *
     * @param other Value to merge this value with.
     * @return Result of modelwise merging of this value with the given value.
     */
    protected AValue modelwiseMerge(ModelableAValue other) {
        if (this.typeName.equals(other.typeName) &&
                this.varName.equals(other.varName)) {
            if (!other.modelingEnabled) {
                return other;
            } else if (!this.modelingEnabled) {
                return this;
            } else if (this.model != other.model) {
                throw new IllegalStateException();
            } else if (this.state == other.state) {
                return other;
            } else {
                return new MultipleValue(this, other);
//				throw new IllegalStateException ();
            }
        } else {
            return new MultipleValue(this, other);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("varName", this.varName).
                append("state", this.state).
                appendSuper(super.toString()).
                toString();
    }
}
