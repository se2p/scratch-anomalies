package org.softevo.oumextractor.modelcreator1.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Serializable;

/**
 * This class is used to represent method calls.
 *
 * @author Andrzej Wasylkowski
 */
public final class MethodCall implements Serializable {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 3444736290963962419L;

    /**
     * Fully qualified name of the type declaring the method.
     */
    private final String typeName;

    /**
     * Name + signature of the method.
     */
    private final String methodName;

    /**
     * Creates new representation of a method call with given description.
     *
     * @param typeName   Fully qualified name of the type declaring the
     *                   method to be called.
     * @param methodName Method name + signature.
     */
    public MethodCall(String typeName, String methodName) {
        this.typeName = typeName;
        this.methodName = methodName;
    }

    /**
     * Returns the method call represented by the given XML element.
     *
     * @param element XML representation of the method call to create.
     * @return Method call, as represented by the given XML element.
     */
    public static MethodCall getFromXMLRepresentation(Element element) {
        assert element.getNodeName().equals("method_call");
        String typeName = element.getAttribute("type_name");
        String methodName = element.getAttribute("method_name");
        return new MethodCall(typeName, methodName);
    }

    /**
     * Returns fully qualified name of the type declaring the called method.
     *
     * @return Fully qualified name of the type declaring the called method.
     */
    public String getTypeName() {
        return this.typeName;
    }

    /**
     * Returns method name + signature of the called method.
     *
     * @return Method name + signature of the called method.
     */
    public String getMethodName() {
        return this.methodName;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MethodCall) {
            MethodCall other = (MethodCall) o;
            return other.typeName.equals(this.typeName) &&
                    other.methodName.equals(this.methodName);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + this.typeName.hashCode();
        result = 37 * result + this.methodName.hashCode();
        return result;
    }

    /**
     * Returns the XML representation of this method call.
     *
     * @param xml Reference XML document.
     * @return XML representation of this method call.
     */
    public Element getXMLRepresentation(Document xml) {
        Element element = xml.createElement("method_call");
        element.setAttribute("type_name", this.typeName);
        element.setAttribute("method_name", this.methodName);
        return element;
    }
}
