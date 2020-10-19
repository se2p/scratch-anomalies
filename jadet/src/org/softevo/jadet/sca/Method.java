package org.softevo.jadet.sca;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.softevo.catools.CAObject;
import org.softevo.jutil.JavaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;


/**
 * Instances of this class are used to represent methods/functions.
 *
 * @author Andrzej Wasylkowski
 */
public class Method implements CAObject {

    /**
     * Mapping from methods' names to their representations.
     */
    private static final Map<String, Method> name2method =
            new HashMap<String, Method>();


    /**
     * Name of the represented method.
     */
    private final String name;


    /**
     * If <code>true</code>, the name has Java semantics and will be parsed.
     */
    private final boolean hasSemantics;


    /**
     * Creates a new representation of the method with the given name.
     *
     * @param name         Name of the method to represent.
     * @param hasSemantics Indicates, if the name has Java semantics.
     */
    private Method(String name, boolean hasSemantics) {
        this.name = name;
        this.hasSemantics = hasSemantics;
    }


    /**
     * Returns representation of the method with the given name.
     *
     * @param name         Name of the method.
     * @param hasSemantics Indicates, if the name has Java semantics.
     * @return Representation of the method.
     */
    public static Method get(String name, boolean hasSemantics) {
        if (name2method.containsKey(name)) {
            Method result = name2method.get(name);
            if (result.hasSemantics != hasSemantics)
                throw new InternalError();
            return result;
        } else {
            Method method = new Method(name, hasSemantics);
            name2method.put(name, method);
            return method;
        }
    }

    /**
     * Returns a method that is represented by the given XML element.
     *
     * @param element Element that contains representation of the method to
     *                be created.
     * @return Method that is represent by the given XML element.
     */
    public static Method createFromXMLElement(Element element) {
        assert element.getTagName().equals("method");
        String name = element.getAttribute("name");
        boolean hasSemantics =
                Boolean.valueOf(element.getAttribute("has_semantics"));
        return get(name, hasSemantics);
    }

    /**
     * Returns a text representation of this method. Length of the
     * representation is determined by the verbosity given.
     *
     * @param verbosity Verbosity of the representation.
     * @return Text representation of this method.
     */
    public String getTextRepresentation(OutputVerbosity verbosity) {
        if (!this.hasSemantics)
            return this.name;

        if (verbosity == OutputVerbosity.FULL) {
            return this.name;
        } else if (verbosity == OutputVerbosity.SHORT ||
                verbosity == OutputVerbosity.VERY_SHORT) {
            return this.name;
        } else {
            System.err.println("Unknown verbosity: " + verbosity);
            throw new InternalError();
        }
    }

    /**
     * Returns the XML representation of this method.
     *
     * @param    xml XML document to use.
     * @return XML representation of this method.
     */
    public Element getXMLRepresentation(Document xml) {
        Element methodXML = xml.createElement("method");

        // set the needed attributes
        methodXML.setAttribute("name", this.name);
        methodXML.setAttribute("has_semantics",
                String.valueOf(this.hasSemantics));

        return methodXML;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 41).
                append(this.name).
                toHashCode();
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Method) {
            Method other = (Method) o;
            return new EqualsBuilder().
                    append(this.name, other.name).
                    isEquals();
        }
        return false;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("name", this.name).
                toString();
    }


    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(CAObject o) {
        Method other = (Method) o;
        return this.name.compareTo(other.name);
    }
}
