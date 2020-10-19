package org.softevo.jadet.sca;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.Set;


/**
 * Instances of this class are used to represent patterns of sequential
 * constraints abstraction.
 *
 * @author Andrzej Wasylkowski
 */
public class Pattern extends org.softevo.catools.Pattern<Method, EventPair> {


    /**
     * Creates a new pattern out of the given pattern.
     *
     * @param pattern Original pattern to represent.
     */
    public Pattern(org.softevo.catools.Pattern<Method, EventPair> pattern) {
        super(pattern.getObjects(), pattern.getProperties(),
                pattern.getSupport());
    }

    /**
     * Creates a pattern out of the given XML element.
     *
     * @param patternXML XML representation of the pattern to create.
     * @return Pattern corresponding to the given XML representation.
     */
    public static Pattern createFromXMLElement(Element patternXML) {
        // get the needed attributes
        int support = Integer.valueOf(patternXML.getAttribute("support"));

        // get all subelements
        Set<Method> supportingMethods = null;
        Set<EventPair> properties = null;
        NodeList patternChildren = patternXML.getChildNodes();
        for (int i = 0; i < patternChildren.getLength(); i++) {
            Node node = patternChildren.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals("supporting_methods")) {
                    assert supportingMethods == null;
                    supportingMethods = getMethodsFromXMLs(element.getChildNodes());
                } else if (element.getTagName().equals("properties")) {
                    assert properties == null;
                    properties = getEventPairsFromXMLs(element.getChildNodes());
                } else {
                    System.err.println("Unknown tag name: " + element.getTagName());
                    throw new InternalError();
                }
            }
        }

        // create the pattern
        org.softevo.catools.Pattern<Method, EventPair> caPattern =
                new org.softevo.catools.Pattern<Method, EventPair>(
                        supportingMethods, properties, support);
        Pattern pattern = new Pattern(caPattern);
        return pattern;
    }

    /**
     * Returns the set of event pairs that occur as top level elements in the
     * given node list.
     *
     * @param nodes List to search through.
     * @return Event pairs that occur as top level elements in the node list.
     */
    private static Set<EventPair> getEventPairsFromXMLs(NodeList nodes) {
        Set<EventPair> pairs = new HashSet<EventPair>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals("event_pair")) {
                    EventPair pair = EventPair.createFromXMLElement(element);
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }

    /**
     * Returns the set of methods that occur as top level elements in the
     * given node list.
     *
     * @param nodes List to search through.
     * @return Methods that occur as top level elements in the node list.
     */
    private static Set<Method> getMethodsFromXMLs(NodeList nodes) {
        Set<Method> methods = new HashSet<Method>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals("method")) {
                    Method method = Method.createFromXMLElement(element);
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    /**
     * Returns the XML representation of this pattern.
     *
     * @param xml XML document to use.
     * @return XML representation of this pattern.
     */
    public Element getXMLRepresentation(Document xml) {
        Element patternXML = xml.createElement("pattern");

        // set the needed attributes
        patternXML.setAttribute("support",
                String.valueOf(getSupport()));

        // add the supporting objects
        Element supportingExamples = xml.createElement("supporting_methods");
        patternXML.appendChild(supportingExamples);
        for (Method method : getObjects()) {
            supportingExamples.appendChild(method.getXMLRepresentation(xml));
        }

        // add the properties
        Element propertiesXML = xml.createElement("properties");
        patternXML.appendChild(propertiesXML);
        for (EventPair pair : getProperties()) {
            propertiesXML.appendChild(pair.getXMLRepresentation(xml));
        }

        return patternXML;
    }
}
