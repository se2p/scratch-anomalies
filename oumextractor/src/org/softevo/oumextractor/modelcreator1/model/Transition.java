package org.softevo.oumextractor.modelcreator1.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to represent transitions between states in a model.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class Transition implements Serializable {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -7751229707515748158L;

    /**
     * Set of all transitions (this is an identity mapping).
     */
    private static transient final Map<Transition, Transition> transitions =
            new HashMap<Transition, Transition>();

    /**
     * Returns transition that is equivalent to the given transition and
     * should be used instead of it.
     *
     * @param t Transition, whose equivalent is to be found.
     * @return Transition equivalent to the given transition;
     */
    public static Transition getTransition(Transition t) {
        if (!transitions.containsKey(t)) {
            transitions.put(t, t);
        }
        return transitions.get(t);
    }

    /**
     * Clears the pool of transitions.
     */
    public static void clearPool() {
        transitions.clear();
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element, or
     * <code>null</code>, if the element was not recognized.
     */
    public static Transition getFromXMLRepresentation(Element element) {
        if (element.getNodeName().equals("cast_transition")) {
            return CastTransition.getFromXMLRepresentation(element);
        } else if (element.getNodeName().equals("epsilon_transition")) {
            return EpsilonTransition.getFromXMLRepresentation(element);
        } else if (element.getNodeName().equals("exception_transition")) {
            return ExceptionTransition.getFromXMLRepresentation(element);
        } else if (element.getNodeName().equals("field_value_transition")) {
            return FieldValueTransition.getFromXMLRepresentation(element);
        } else if (element.getNodeName().equals("invoke_method_transition")) {
            return InvokeMethodTransition.getFromXMLRepresentation(element);
        } else if (element.getNodeName().equals("return_value_of_method_transition")) {
            return ReturnValueOfMethodTransition.getFromXMLRepresentation(element);
        } else if (element.getNodeName().equals("lightweight_transition")) {
            return LightweightTransition.getFromXMLRepresentation(element);
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public abstract boolean equals(Object o);

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public abstract int hashCode();

    /**
     * Returns 'dot' representation of this transition as a 'dot' label.
     *
     * @return 'dot' label being a representation of this transition.
     */
    public abstract String getDotString();

    /**
     * Returns the XML representation of this transition.
     *
     * @param xml Reference XML document.
     * @return XML representation of this transition.
     */
    public abstract Element getXMLRepresentation(Document xml);

    /**
     * Returns string that fully represents the event denoted by this
     * transition in a human-readable way. This string should uniquely identify
     * the transition. It can be empty for the epsilon transition only.
     */
    public abstract String getLongEventString();

    /**
     * Returns string that represents the event denoted by this transition
     * in a human-readable way, but with classes' names in a signature given
     * without packages' names. This string does not always uniquely identify
     * the transition. It can be empty for the epsilon transition only.
     */
    public abstract String getShortEventString();

    /**
     * Returns string that represents the event denoted by this transition
     * in a human-readable way, but with all classes' names given without
     * packages' names. This string does not always uniquely identify the
     * transition. It can be empty for the epsilon transition only.
     */
    public abstract String getVeryShortEventString();
}
