package org.softevo.jadet.sca;


import org.softevo.catools.Pattern;
import org.softevo.catools.Pattern.PatternComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;


/**
 * Instances of this class are used to represent violations of sequential
 * constraints abstraction patterns.
 *
 * @author Andrzej Wasylkowski
 */
public class Violation extends org.softevo.catools.Violation<Method, EventPair> {

    /**
     * Defect indicator of this violation, or <code>null</code> if there is none.
     */
    private Double defectIndicator;


    /**
     * Type of this violation
     */
    private ViolationType type;


    /**
     * Description of this violation.
     */
    private String description;


    /**
     * Creates a new violation out of the given violation.
     *
     * @param violation Original violation to represent.
     */
    public Violation(org.softevo.catools.Violation<Method, EventPair> violation) {
        super(violation.getPattern(), violation.getObject(),
                violation.getMissingProperties(), violation.getConfidence());
        this.defectIndicator = null;
        this.type = ViolationType.UNKNOWN;
        this.description = "";
    }

    /**
     * Creates a violation out of the given XML element.
     *
     * @param violationXML XML representation of the violation to create.
     * @return Violation corresponding to the given XML representation.
     */
    public static Violation createFromXMLElement(Element violationXML) {
        // get the needed attributes
        int support = Integer.valueOf(violationXML.getAttribute("support"));
        double confidence =
                Double.valueOf(violationXML.getAttribute("confidence"));
        double defectIndicator =
                Double.valueOf(violationXML.getAttribute("defect_indicator"));
        ViolationType type =
                ViolationType.valueOf(violationXML.getAttribute("type"));

        // get all subelements
        String description = null;
        Method violatingObject = null;
        Set<Method> supportingMethods = null;
        Set<EventPair> presentProperties = null;
        Set<EventPair> missingProperties = null;
        NodeList violationChildren = violationXML.getChildNodes();
        for (int i = 0; i < violationChildren.getLength(); i++) {
            Node node = violationChildren.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals("method")) {
                    assert violatingObject == null;
                    violatingObject = Method.createFromXMLElement(element);
                } else if (element.getTagName().equals("supporting_methods")) {
                    assert supportingMethods == null;
                    supportingMethods = getMethodsFromXMLs(element.getChildNodes());
                } else if (element.getTagName().equals("present_properties")) {
                    assert presentProperties == null;
                    presentProperties = getEventPairsFromXMLs(element.getChildNodes());
                } else if (element.getTagName().equals("missing_properties")) {
                    assert missingProperties == null;
                    missingProperties = getEventPairsFromXMLs(element.getChildNodes());
                } else if (element.getTagName().equals("description")) {
                    assert description == null;
                    description = element.getAttribute("value");
                } else {
                    System.err.println("Unknown tag name: " + element.getTagName());
                    throw new InternalError();
                }
            }
        }

        // create the violation
        Set<EventPair> allProperties = new HashSet<EventPair>();
        allProperties.addAll(presentProperties);
        allProperties.addAll(missingProperties);
        Pattern<Method, EventPair> pattern = new Pattern<Method, EventPair>(
                supportingMethods, allProperties, support);
        org.softevo.catools.Violation<Method, EventPair> caViolation =
                new org.softevo.catools.Violation<Method, EventPair>(pattern,
                        violatingObject, missingProperties, confidence);
        Violation violation = new Violation(caViolation);
        violation.setDefectIndicator(defectIndicator);
        violation.setDescription(description);
        violation.setType(type);
        return violation;
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
     * Gets the defect indicator of this violation.
     *
     * @return Defect indicator of this violation.
     */
    public Double getDefectIndicator() {
        return this.defectIndicator;
    }

    /**
     * Sets the defect indicator to the given value.
     *
     * @param defectIndicator Defect indicator of this violation.
     */
    public void setDefectIndicator(Double defectIndicator) {
        this.defectIndicator = defectIndicator;
    }

    /**
     * Returns the type of this violation.
     *
     * @return The type of this violation.
     */
    public ViolationType getType() {
        return this.type;
    }

    /**
     * Sets the type of this violation to a given one.
     *
     * @param type Type of the violation.
     */
    public void setType(ViolationType type) {
        this.type = type;
    }

    /**
     * Returns the description of this violation.
     *
     * @return Description of this violation.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description of this violation to the given one.
     *
     * @param description New description of this violation.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the XML representation of this violation.
     *
     * @param xml XML document to use.
     * @return XML representation of this violation.
     */
    public Element getXMLRepresentation(Document xml) {
        Element violationXML = xml.createElement("violation");

        // set the needed attributes
        violationXML.setAttribute("support",
                String.valueOf(this.getPattern().getSupport()));
        violationXML.setAttribute("confidence",
                String.valueOf(this.getConfidence()));
        violationXML.setAttribute("defect_indicator",
                String.valueOf(this.defectIndicator));
        violationXML.setAttribute("type", String.valueOf(this.type));
        Element descriptionXML = xml.createElement("description");
        descriptionXML.setAttribute("value", this.description);
        violationXML.appendChild(descriptionXML);

        // add the violating object
        violationXML.appendChild(this.getObject().getXMLRepresentation(xml));

        // add the supporting objects
        Element supportingExamples = xml.createElement("supporting_methods");
        violationXML.appendChild(supportingExamples);
        for (Method method : this.getPattern().getObjects()) {
            supportingExamples.appendChild(method.getXMLRepresentation(xml));
        }

        // add the present properties
        Element presentPropertiesXML = xml.createElement("present_properties");
        violationXML.appendChild(presentPropertiesXML);
        for (EventPair pair : this.getPattern().getProperties()) {
            if (!this.getMissingProperties().contains(pair)) {
                presentPropertiesXML.appendChild(pair.getXMLRepresentation(xml));
            }
        }

        // add the missing properties
        Element missingPropertiesXML = xml.createElement("missing_properties");
        violationXML.appendChild(missingPropertiesXML);
        for (EventPair pair : this.getMissingProperties()) {
            missingPropertiesXML.appendChild(pair.getXMLRepresentation(xml));
        }

        return violationXML;
    }

    /**
     * This class is responsible for comparing violations according to their
     * defect indicator measures, and then their pattern ordering.
     *
     * @author Andrzej Wasylkowski
     */
    public static class ViolationComparator implements Comparator<Violation> {

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Violation v1, Violation v2) {
            int diCompare = v1.getDefectIndicator().compareTo(v2.getDefectIndicator());
            if (diCompare != 0) {
                return -diCompare;
            } else {
                PatternComparator<Method, EventPair> comparator =
                        new PatternComparator<Method, EventPair>();
                return comparator.compare(v1.getPattern(), v2.getPattern());
            }
        }
    }
}
