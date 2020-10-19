package org.softevo.oumextractor.modelcreator1.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Instances of this class are used to represent lightweight transitions.
 *
 * @author Andrzej Wasylkowski
 */
public class LightweightTransition extends Transition {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 261569942842063722L;

    /**
     * Regex pattern for events.
     */
    private static Pattern eventPattern = Pattern.compile("([^\\(]+)\\((\\d+)\\)@(-?\\d+)");

    /**
     * Cached hash code of this transition.
     */
    private transient Integer hashCode = null;

    /**
     * Cached event string of this transition.
     */
    private transient String eventString = null;

    /**
     * Name of the function.
     */
    private String functionName;

    /**
     * Number of parameters the function takes.
     */
    private int parametersNum;

    /**
     * Number of parameter used in the event.
     */
    private int parameter;

    /**
     * Creates a lightweight transition out of the given event.
     *
     * @param event Event to represent.
     */
    private LightweightTransition(String event) {
        Matcher m = eventPattern.matcher(event);
        if (!m.matches()) {
            System.err.println("Incorrect event format: " + event);
            throw new InternalError();
        }
        this.functionName = m.group(1);
        this.parametersNum = Integer.valueOf(m.group(2));
        this.parameter = Integer.valueOf(m.group(3));
    }

    /**
     * Returns a lightweight transition representing the given event.
     *
     * @param event Event to represent.
     */
    public static LightweightTransition get(String event) {
        Transition t = new LightweightTransition(event);
        return (LightweightTransition) Transition.getTransition(t);
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element.
     */
    public static LightweightTransition getFromXMLRepresentation(Element element) {
        assert element.getNodeName().equals("lightweight_transition");
        String functionName = element.getAttribute("function_name");
        int parametersNum =
                Integer.valueOf(element.getAttribute("parameters_num"));
        int parameter = Integer.valueOf(element.getAttribute("parameter"));
        return get(functionName + "(" + parametersNum + ")@" + parameter);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof LightweightTransition) {
            LightweightTransition other = (LightweightTransition) o;
            return new EqualsBuilder().
                    append(this.functionName, other.functionName).
                    append(this.parametersNum, other.parametersNum).
                    append(this.parameter, other.parameter).
                    isEquals();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getDotString()
     */
    @Override
    public String getDotString() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getLongEventString()
     */
    @Override
    public String getLongEventString() {
        if (this.eventString == null) {
            if (this.parameter == -1)
                this.eventString = "RETVAL: " + this.functionName + "(" +
                        this.parametersNum + ")";
            else
                this.eventString = this.functionName + "(" +
                        this.parametersNum + ") @ " + this.parameter;
        }
        return this.eventString;
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

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getXMLRepresentation(org.w3c.dom.Document)
     */
    @Override
    public Element getXMLRepresentation(Document xml) {
        Element transitionElement = xml.createElement("lightweight_transition");
        transitionElement.setAttribute("function_name", this.functionName);
        transitionElement.setAttribute("parameters_num",
                String.valueOf(this.parametersNum));
        transitionElement.setAttribute("parameter",
                String.valueOf(this.parameter));
        return transitionElement;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = new HashCodeBuilder(9, 37).
                    append(this.functionName).
                    append(this.parametersNum).
                    append(this.parameter).
                    toHashCode();
        }
        return this.hashCode;
    }

    protected Object readResolve() {
        return (LightweightTransition) Transition.getTransition(this);
    }
}
