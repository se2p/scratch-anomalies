package org.softevo.oumextractor.modelcreator1.model;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.softevo.jutil.JavaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is used to represent transitions in a model caused by casting
 * the object.
 *
 * @author Andrzej Wasylkowski
 */
public final class CastTransition extends Transition {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -4460476721538101549L;

    /**
     * Type, to which the object was cast.
     */
    private final String type;

    /**
     * Cached hash code of this transition.
     */
    private transient Integer hashCode = null;

    /**
     * Cached long (=short) event string of this transition.
     */
    private transient String longEventString = null;

    /**
     * Cached very short event string of this transition.
     */
    private transient String veryShortEventString = null;

    /**
     * Creates new transition caused by casting the object to the given type.
     *
     * @param type Type, to which the object was cast.
     */
    private CastTransition(String type) {
        this.type = type;
    }

    /**
     * Returns transition caused by invoking given method with object
     * passed as the given set of parameters.
     *
     * @param methodCall       Method call to represent.
     * @param parameterIndices Indices of parameters used.
     */
    public static CastTransition get(String type) {
        Transition t = new CastTransition(type);
        return (CastTransition) Transition.getTransition(t);
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element.
     */
    public static CastTransition getFromXMLRepresentation(Element element) {
        assert element.getNodeName().equals("cast_transition");
        String type = element.getAttribute("type");
        return get(type);
    }

    /**
     * Returns the fully qualified name of the type, to which the object
     * was cast.
     *
     * @return Type, to which the object was cast.
     */
    public String getType() {
        return this.type;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CastTransition) {
            CastTransition other = (CastTransition) o;
            return this.type.equals(other.type);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = new HashCodeBuilder(11, 37).
                    append(this.type).
                    toHashCode();
        }
        return this.hashCode;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getDotString()
     */
    @Override
    public String getDotString() {
        StringBuffer result = new StringBuffer();
        result.append("CAST: ");
        result.append(JavaUtil.getSimpleClassName(this.type));
        return result.toString();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getXMLRepresentation(org.w3c.dom.Document)
     */
    @Override
    public Element getXMLRepresentation(Document xml) {
        Element castElement = xml.createElement("cast_transition");
        castElement.setAttribute("type", this.type);
        return castElement;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getLongEventString()
     */
    @Override
    public String getLongEventString() {
        if (this.longEventString == null) {
            StringBuffer result = new StringBuffer();
            result.append("CAST: ");
            result.append(this.type);
            this.longEventString = result.toString();
        }
        return this.longEventString;
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
        if (this.veryShortEventString == null) {
            StringBuffer result = new StringBuffer();
            result.append("CAST: ");
            result.append(JavaUtil.getSimpleClassName(this.type));
            this.veryShortEventString = result.toString();
        }
        return this.veryShortEventString;
    }

    protected Object readResolve() {
        return (CastTransition) Transition.getTransition(this);
    }
}
