package org.softevo.oumextractor.modelcreator1.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is used to represent "epsilon transitions" in a model.  It can
 * only be used as a singleton.
 *
 * @author Andrzej Wasylkowski
 */
public final class EpsilonTransition extends Transition {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 2975260008494701556L;

    /**
     * Creates new "epsilon transition".
     */
    private EpsilonTransition() {
    }

    /**
     * Returns the epsilon transition
     */
    public static EpsilonTransition get() {
        return (EpsilonTransition)
                Transition.getTransition(new EpsilonTransition());
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element.
     */
    public static EpsilonTransition getFromXMLRepresentation(Element element) {
        System.err.println(element.getNodeName());
        assert element.getNodeName().equals("epsilon_transition");
        return get();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof EpsilonTransition) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 1;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getDotString()
     */
    @Override
    public String getDotString() {
        return "ε";
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getXMLRepresentation(org.w3c.dom.Document)
     */
    @Override
    public Element getXMLRepresentation(Document xml) {
        Element epsilonElement = xml.createElement("epsilon_transition");
        return epsilonElement;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getLongEventString()
     */
    @Override
    public String getLongEventString() {
        return "";
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getShortEventString()
     */
    @Override
    public String getShortEventString() {
        return getLongEventString();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getVeryShortEventString()
     */
    @Override
    public String getVeryShortEventString() {
        return getLongEventString();
    }

    protected Object readResolve() {
        return (EpsilonTransition) Transition.getTransition(this);
    }
}
