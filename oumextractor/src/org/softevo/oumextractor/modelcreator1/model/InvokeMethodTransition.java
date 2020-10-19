package org.softevo.oumextractor.modelcreator1.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is used to represent transitions in a model caused by invoking methods
 * on an object.
 *
 * @author Andrzej Wasylkowski
 */
public final class InvokeMethodTransition extends Transition {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 534106845338350710L;

    /**
     * Represented method call.
     */
    private final MethodCall methodCall;

    /**
     * Indices of parameters used.
     */
    private final List<Integer> parameterIndices;

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
     * Creates new transition caused by invoking given method with object
     * passed as the given set of parameters.
     *
     * @param methodCall       Method call to represent.
     * @param parameterIndices Indices of parameters used.
     */
    private InvokeMethodTransition(MethodCall methodCall,
                                   List<Integer> parameterIndices) {
        this.methodCall = methodCall;
        this.parameterIndices = new ArrayList<Integer>(parameterIndices);
    }

    /**
     * Returns transition caused by invoking given method with object
     * passed as the given set of parameters.
     *
     * @param methodCall       Method call to represent.
     * @param parameterIndices Indices of parameters used.
     */
    public static InvokeMethodTransition get(MethodCall methodCall,
                                             List<Integer> parameterIndices) {
        Transition t = new InvokeMethodTransition(methodCall, parameterIndices);
        return (InvokeMethodTransition) Transition.getTransition(t);
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element.
     */
    public static InvokeMethodTransition getFromXMLRepresentation(Element element) {
        assert element.getNodeName().equals("invoke_method_transition");
        NodeList childNodes = element.getChildNodes();
        MethodCall methodCall = null;
        List<Integer> parameterIndices = new ArrayList<Integer>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("parameter")) {
                Element parameter = (Element) node;
                Integer index = Integer.valueOf(parameter.getAttribute("index"));
                parameterIndices.add(index);
            } else if (node instanceof Element) {
                assert methodCall == null;
                methodCall = MethodCall.getFromXMLRepresentation((Element) node);
            }
        }
        assert methodCall != null;
        return get(methodCall, parameterIndices);
    }

    /**
     * Returns the method call represent by this transition.
     *
     * @return Method call represented by this transition.
     */
    public MethodCall getMethodCall() {
        return this.methodCall;
    }

    /**
     * Returns the list of indices of parameters used by the object being
     * modeled for this method call.
     *
     * @return List of indices of parameters used by the object being modeled.
     */
    public List<Integer> getParameterIndices() {
        return Collections.unmodifiableList(this.parameterIndices);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof InvokeMethodTransition) {
            InvokeMethodTransition other = (InvokeMethodTransition) o;
            return this.methodCall.equals(other.methodCall) &&
                    this.parameterIndices.equals(other.parameterIndices);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            int result = 5;
            result = 37 * result + this.methodCall.hashCode();
            result = 37 * result + this.parameterIndices.hashCode();
            this.hashCode = result;
        }
        return this.hashCode;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getDotString()
     */
    @Override
    public String getDotString() {
        return this.methodCall.getMethodName();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getXMLRepresentation(org.w3c.dom.Document)
     */
    @Override
    public Element getXMLRepresentation(Document xml) {
        Element invokeMethodElement = xml.createElement("invoke_method_transition");
        invokeMethodElement.appendChild(this.methodCall.getXMLRepresentation(xml));
        for (Integer index : this.parameterIndices) {
            Element indexElement = xml.createElement("parameter");
            indexElement.setAttribute("index", index.toString());
            invokeMethodElement.appendChild(indexElement);
        }
        return invokeMethodElement;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getLongEventString()
     */
    @Override
    public String getLongEventString() {
        return this.methodCall.getMethodName();

//        if (this.longEventString == null) {
//            StringBuffer result = new StringBuffer();
//            result.append(this.methodCall.getTypeName()).append('.');
//            result.append(JavaUtil.methodInternalToExternal(
//                    this.methodCall.getMethodName(), false));
//            result.append(" @ (");
//            for (int index : this.parameterIndices) {
//                result.append(index);
//                result.append(", ");
//            }
//            result.delete(result.length() - 2, result.length());
//            result.append(')');
//            this.longEventString = result.toString();
//        }
//        return this.longEventString;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getShortEventString()
     */
    @Override
    public String getShortEventString() {
        return this.methodCall.getMethodName();
//
//        if (this.shortEventString == null) {
//            StringBuffer result = new StringBuffer();
//            result.append(this.methodCall.getTypeName()).append('.');
//            result.append(JavaUtil.methodInternalToExternal(
//                    this.methodCall.getMethodName(), true));
//            result.append(" @ (");
//            for (int index : this.parameterIndices) {
//                result.append(index);
//                result.append(", ");
//            }
//            result.delete(result.length() - 2, result.length());
//            result.append(')');
//            this.shortEventString = result.toString();
//        }
//        return this.shortEventString;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getVeryShortEventString()
     */
    @Override
    public String getVeryShortEventString() {
        return this.methodCall.getMethodName();
        /*if (this.veryShortEventString == null) {
            StringBuffer result = new StringBuffer();
            result.append(JavaUtil.getSimpleClassName(
                    this.methodCall.getTypeName())).append('.');
            result.append(JavaUtil.methodInternalToExternal(
                    this.methodCall.getMethodName(), true));
            result.append(" @ (");
            for (int index : this.parameterIndices) {
                result.append(index);
                result.append(", ");
            }
            result.delete(result.length() - 2, result.length());
            result.append(')');
            this.veryShortEventString = result.toString();
        }
        return this.veryShortEventString;*/
    }

    protected Object readResolve() {
        return (InvokeMethodTransition) Transition.getTransition(this);
    }
}
