package org.softevo.jadet.sca;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.softevo.catools.CAProperty;
import org.softevo.oumextractor.modelcreator1.model.Transition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;


/**
 * Instances of this class represent pairs of events used by the sequential
 * constraints abstraction.
 *
 * @author Andrzej Wasylkowski
 */
public class EventPair implements CAProperty {

    /**
     * Mapping from event pairs to themselves (used by the factory).
     */
    private static final Map<EventPair, EventPair> pair2pair =
            new HashMap<EventPair, EventPair>();


    /**
     * Event on the left-hand side.
     */
    private final Transition left;


    /**
     * Event on the right-hand side.
     */
    private final Transition right;


    /**
     * Creates a pair of given events.
     *
     * @param left  Event on the left-hand side.
     * @param right Event on the right-hand side.
     */
    private EventPair(Transition left, Transition right) {
        this.left = left;
        this.right = right;
    }


    /**
     * Returns a pair of given events.
     *
     * @param left  Event on the left-hand side.
     * @param right Event on the right-hand side.
     * @return Pair of given events.
     */
    public static EventPair get(Transition left, Transition right) {
        EventPair pair = new EventPair(left, right);
        if (pair2pair.containsKey(pair)) {
            return pair2pair.get(pair);
        } else {
            pair2pair.put(pair, pair);
            return pair;
        }
    }

    /**
     * Returns an event pair that is represented by the given XML element.
     *
     * @param element Element that contains representation of the event pair
     *                to be created.
     * @return Event pair that is represent by the given XML element.
     */
    public static EventPair createFromXMLElement(Element element) {
        assert element.getTagName().equals("event_pair");
        Transition left = null;
        Transition right = null;
        NodeList events = element.getChildNodes();
        for (int i = 0; i < events.getLength(); i++) {
            Node node = events.item(i);
            if (node instanceof Element) {
                Element eventElement = (Element) node;
                if (eventElement.getTagName().equals("left")) {
                    NodeList leftEvents = eventElement.getChildNodes();
                    for (int j = 0; j < events.getLength(); j++) {
                        Node leftNode = leftEvents.item(j);
                        if (leftNode instanceof Element) {
                            assert left == null;
                            left = Transition.getFromXMLRepresentation(
                                    (Element) leftNode);
                        }
                    }
                } else if (eventElement.getTagName().equals("right")) {
                    NodeList rightEvents = eventElement.getChildNodes();
                    for (int j = 0; j < events.getLength(); j++) {
                        Node rightNode = rightEvents.item(j);
                        if (rightNode instanceof Element) {
                            assert right == null;
                            right = Transition.getFromXMLRepresentation(
                                    (Element) rightNode);
                        }
                    }
                } else {
                    System.err.println("Unknown node: " +
                            eventElement.getTagName());
                }
            }
        }
        return get(left, right);
    }

    /**
     * Returns the left-hand side of this event pair.
     *
     * @return Left-hand side of this event pair.
     */
    public Transition getLeft() {
        return this.left;
    }

    /**
     * Returns the right-hand side of this event pair.
     *
     * @return Right-hand side of this event pair.
     */
    public Transition getRight() {
        return this.right;
    }

    /**
     * Returns a text representation of this event pair. Length of the
     * representation is determined by the verbosity given.
     *
     * @param verbosity Verbosity of the representation.
     * @return Text representation of this event pair.
     */
    public String getTextRepresentation(OutputVerbosity verbosity) {
        if (verbosity == OutputVerbosity.FULL) {
            return this.left.getLongEventString() + " < " +
                    this.right.getLongEventString();
        } else if (verbosity == OutputVerbosity.SHORT) {
            return this.left.getShortEventString() + " < " +
                    this.right.getShortEventString();
        } else if (verbosity == OutputVerbosity.VERY_SHORT) {
            return this.left.getVeryShortEventString() + " < " +
                    this.right.getVeryShortEventString();
        } else {
            System.err.println("Unknown verbosity: " + verbosity);
            throw new InternalError();
        }
    }

    /**
     * Returns the XML representation of this event pair.
     *
     * @param    xml XML document to use.
     * @return XML representation of this event pair.
     */
    public Element getXMLRepresentation(Document xml) {
        Element pairXML = xml.createElement("event_pair");

        // set the needed attributes
        Element leftXML = xml.createElement("left");
        leftXML.appendChild(this.left.getXMLRepresentation(xml));
        pairXML.appendChild(leftXML);

        Element rightXML = xml.createElement("right");
        rightXML.appendChild(this.right.getXMLRepresentation(xml));
        pairXML.appendChild(rightXML);

        return pairXML;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 39).
                append(this.left).
                append(this.right).
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
        if (o instanceof EventPair) {
            EventPair other = (EventPair) o;
            return new EqualsBuilder().
                    append(this.left, other.left).
                    append(this.right, other.right).
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
                append("left", this.left.getLongEventString()).
                append("right", this.right.getLongEventString()).
                toString();
    }


    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(CAProperty o) {
        EventPair other = (EventPair) o;
        int cmp1 = this.left.getLongEventString().compareTo(
                other.left.getLongEventString());
        if (cmp1 != 0) {
            return cmp1;
        } else {
            return this.right.getLongEventString().compareTo(
                    other.right.getLongEventString());
        }
    }
}
