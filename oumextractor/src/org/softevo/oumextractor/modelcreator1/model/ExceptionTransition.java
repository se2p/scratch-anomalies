package org.softevo.oumextractor.modelcreator1.model;

import org.softevo.jutil.JavaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is used to represent transitions ending with an exception being thrown.
 *
 * @author Andrzej Wasylkowski
 */
public final class ExceptionTransition extends Transition {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 4670603099389232571L;

    /**
     * Encapsulated transition.
     */
    private final Transition transition;

    /**
     * Fully qualified name of an exception class.
     */
    private final String excType;

    /**
     * Cached hash code of this transition.
     */
    private transient Integer hashCode = null;

    /**
     * Cached long event string of this transition.
     */
    private transient String longEventString = null;

    /**
     * Cached short event string of this transition.
     */
    private transient String shortEventString = null;

    /**
     * Cached very short event string of this transition.
     */
    private transient String veryShortEventString = null;

    /**
     * Creates new transition encapsulating given transition.
     *
     * @param transition Transition to encapsulate.
     * @param excType    Fully qualified name of an exception class.
     */
    private ExceptionTransition(Transition transition, String excType) {
        this.transition = transition;
        this.excType = excType;
    }

    /**
     * Returns transition encapsulating given transition.
     *
     * @param transition Transition to encapsulate.
     * @param excType    Fully qualified name of an exception class.
     */
    public static ExceptionTransition get(Transition transition,
                                          String excType) {
        Transition t = new ExceptionTransition(transition, excType);
        return (ExceptionTransition) Transition.getTransition(t);
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element.
     */
    public static ExceptionTransition getFromXMLRepresentation(Element element) {
        assert element.getNodeName().equals("exception_transition");
        String excType = element.getAttribute("exc_type");
        Transition transition = null;
        NodeList transitionNodes = element.getChildNodes();
        for (int i = 0; i < transitionNodes.getLength(); i++) {
            Node node = transitionNodes.item(i);
            if (node instanceof Element) {
                Element transitionXML = (Element) node;
                assert transition == null;
                transition = Transition.getFromXMLRepresentation(transitionXML);
            }
        }
        return get(transition, excType);
    }

    /**
     * Returns encapsulated transition.
     *
     * @return Encapsulated transition.
     */
    public Transition getTransition() {
        return this.transition;
    }

    /**
     * Returns fully qualified name of an exception class.
     *
     * @return Fully qualified name of an exception class.
     */
    public String getExceptionType() {
        return this.excType;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ExceptionTransition) {
            ExceptionTransition other = (ExceptionTransition) o;
            return this.transition.equals(other.transition) &&
                    this.excType.equals(other.excType);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            int result = 2;
            result = 37 * result + this.transition.hashCode();
            result = 37 * result + this.excType.hashCode();
            this.hashCode = result;
        }
        return this.hashCode;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getDotString()
     */
    @Override
    public String getDotString() {
        return "EXC(" + JavaUtil.getSimpleClassName(this.excType) + "): " +
                this.transition.getDotString();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getXMLRepresentation(org.w3c.dom.Document)
     */
    @Override
    public Element getXMLRepresentation(Document xml) {
        Element exceptionElement = xml.createElement("exception_transition");
        exceptionElement.setAttribute("exc_type", this.excType);
        exceptionElement.appendChild(this.transition.getXMLRepresentation(xml));
        return exceptionElement;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getLongEventString()
     */
    @Override
    public String getLongEventString() {
        if (this.longEventString == null) {
            String transitionEventString = this.transition.getLongEventString();
            this.longEventString = "EXC(" + this.excType + "): " +
                    transitionEventString;
        }
        return this.longEventString;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getShortEventString()
     */
    @Override
    public String getShortEventString() {
        if (this.shortEventString == null) {
            String transitionEventString = this.transition.getShortEventString();
            this.shortEventString = "EXC(" + this.excType + "): " +
                    transitionEventString;
        }
        return this.shortEventString;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getVeryShortEventString()
     */
    @Override
    public String getVeryShortEventString() {
        if (this.veryShortEventString == null) {
            String transitionEventString = this.transition.getVeryShortEventString();
            String pureExcType = JavaUtil.getSimpleClassName(this.excType);
            this.veryShortEventString = "EXC(" + pureExcType + "): " +
                    transitionEventString;
        }
        return this.veryShortEventString;
    }

    protected Object readResolve() {
        return (ExceptionTransition) Transition.getTransition(this);
    }
}
