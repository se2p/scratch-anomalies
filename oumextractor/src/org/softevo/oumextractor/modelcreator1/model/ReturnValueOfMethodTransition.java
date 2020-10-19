package org.softevo.oumextractor.modelcreator1.model;

import org.softevo.jutil.JavaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is used to represent transitions in a model caused by the object
 * being modeled being a return value of a method invocation.
 *
 * @author Andrzej Wasylkowski
 */
public final class ReturnValueOfMethodTransition extends Transition {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -539490392807606050L;

    /**
     * Represented method call.
     */
    private final MethodCall methodCall;

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
     * Creates new transition caused by invoking given method with given
     * parameters.
     *
     * @param methodCall Method call.
     */
    private ReturnValueOfMethodTransition(MethodCall methodCall) {
        this.methodCall = methodCall;
    }

    /**
     * Returns transition caused by invoking given method with given parameters.
     *
     * @param methodCall Method call.
     */
    public static ReturnValueOfMethodTransition get(MethodCall methodCall) {
        Transition t = new ReturnValueOfMethodTransition(methodCall);
        return (ReturnValueOfMethodTransition) Transition.getTransition(t);
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element.
     */
    public static ReturnValueOfMethodTransition getFromXMLRepresentation(
            Element element) {
        assert element.getNodeName().equals("return_value_of_method_transition");
        MethodCall methodCall = null;
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                assert methodCall == null;
                methodCall = MethodCall.getFromXMLRepresentation(
                        (Element) node);
            }
        }
        return get(methodCall);
    }

    /**
     * Returns the method call encapsulated in this transition.
     *
     * @return Method call encapsulated in this transition.
     */
    public MethodCall getMethodCall() {
        return this.methodCall;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ReturnValueOfMethodTransition) {
            ReturnValueOfMethodTransition other = (ReturnValueOfMethodTransition) o;
            return this.methodCall.equals(other.methodCall);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            int result = 6;
            result = 37 * result + this.methodCall.hashCode();
            this.hashCode = result;
        }
        return this.hashCode;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getDotString()
     */
    @Override
    public String getDotString() {
        StringBuffer result = new StringBuffer("RETVAL: ");
        result.append(JavaUtil.getSimpleClassName(
                this.methodCall.getTypeName())).append('.');
        result.append(JavaUtil.getSimpleMethodName(
                this.methodCall.getMethodName()));
        return result.toString();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getXMLRepresentation(org.w3c.dom.Document)
     */
    @Override
    public Element getXMLRepresentation(Document xml) {
        Element returnValueOfMethodElement = xml.createElement("return_value_of_method_transition");
        returnValueOfMethodElement.appendChild(
                this.methodCall.getXMLRepresentation(xml));
        return returnValueOfMethodElement;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getLongEventString()
     */
    @Override
    public String getLongEventString() {
        if (this.longEventString == null) {
            StringBuffer result = new StringBuffer("RETVAL: ");
            result.append(this.methodCall.getTypeName()).append('.');
            result.append(JavaUtil.methodInternalToExternal(
                    this.methodCall.getMethodName(), false));
            this.longEventString = result.toString();
        }
        return this.longEventString;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getShortEventString()
     */
    @Override
    public String getShortEventString() {
        if (this.shortEventString == null) {
            StringBuffer result = new StringBuffer("RETVAL: ");
            result.append(this.methodCall.getTypeName()).append('.');
            result.append(JavaUtil.methodInternalToExternal(
                    this.methodCall.getMethodName(), true));
            this.shortEventString = result.toString();
        }
        return this.shortEventString;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getVeryShortEventString()
     */
    @Override
    public String getVeryShortEventString() {
        if (this.veryShortEventString == null) {
            StringBuffer result = new StringBuffer("RETVAL: ");
            result.append(JavaUtil.getSimpleClassName(
                    this.methodCall.getTypeName())).append('.');
            result.append(JavaUtil.methodInternalToExternal(
                    this.methodCall.getMethodName(), true));
            this.veryShortEventString = result.toString();
        }
        return this.veryShortEventString;
    }

    protected Object readResolve() {
        return (ReturnValueOfMethodTransition) Transition.getTransition(this);
    }
}
