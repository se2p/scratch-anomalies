package org.softevo.oumextractor.modelcreator1.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.softevo.jutil.JavaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Instances of this class are used to represent transitions stemming from a
 * value being read from a field.
 *
 * @author Andrzej Wasylkowski
 */
public class FieldValueTransition extends Transition {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 5707888030050122667L;

    /**
     * Full name of the field the value was read from.
     */
    private final String fieldName;

    /**
     * Internal name of the type of the field.
     */
    private final String fieldType;

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
     * Creates new transition representing reading a value from a given field.
     *
     * @param fieldName Full name of the field (including full name of the
     *                  class the field is defined in).
     * @param fieldType Internal name of the type of the field.
     */
    private FieldValueTransition(String fieldName, String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    /**
     * Returns transition representing reading a value from a given field.
     *
     * @param fieldName Full name of the field (including full name of the
     *                  class the field is defined in).
     * @param typeName  Internal name of the type of the field.
     */
    public static FieldValueTransition get(String fieldName, String typeName) {
        Transition t = new FieldValueTransition(fieldName, typeName);
        return (FieldValueTransition) Transition.getTransition(t);
    }

    /**
     * Returns (creating it, if necessary) the transition represented by the
     * given XML element.
     *
     * @param element XML representation of the transition to create.
     * @return Transition, as represented by the given XML element.
     */
    public static FieldValueTransition getFromXMLRepresentation(Element element) {
        assert element.getNodeName().equals("field_value_transition");
        String fieldType = element.getAttribute("field_type");
        String fieldName = element.getAttribute("field_name");
        return get(fieldName, fieldType);
    }

    /**
     * Returns the name of the type of the field, from which the value was read.
     *
     * @return Name of the type of the field, from which the value was read.
     */
    public String getFieldType() {
        return JavaUtil.typeInternalToExternal(this.fieldType);
    }

    /**
     * Returns the full name of the read field (class name + "." + actual field
     * name).
     *
     * @return Full name of the read field.
     */
    public String getFieldName() {
        return this.fieldName;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof FieldValueTransition) {
            FieldValueTransition other = (FieldValueTransition) o;
            return new EqualsBuilder().
                    append(this.fieldName, other.fieldName).
                    append(this.fieldType, other.fieldType).
                    isEquals();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#hashCode()
     */
    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = new HashCodeBuilder(9, 37).
                    append(this.fieldName).
                    append(this.fieldType).
                    toHashCode();
        }
        return this.hashCode;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getDotString()
     */
    @Override
    public String getDotString() {
        int lastDotIndex = this.fieldName.lastIndexOf('.');
        String className = this.fieldName.substring(0, lastDotIndex);
        String simpleFieldName = this.fieldName.substring(lastDotIndex + 1);
        return "FIELDVAL: " + JavaUtil.getSimpleClassName(className) + '.' +
                simpleFieldName;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getXMLRepresentation(org.w3c.dom.Document)
     */
    @Override
    public Element getXMLRepresentation(Document xml) {
        Element fieldValueElement = xml.createElement("field_value_transition");
        fieldValueElement.setAttribute("field_type", this.fieldType);
        fieldValueElement.setAttribute("field_name", this.fieldName);
        return fieldValueElement;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.Transition#getLongEventString()
     */
    @Override
    public String getLongEventString() {
        if (this.longEventString == null) {
            this.longEventString = "FIELDVAL: " + this.fieldName;
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
            int lastDotIndex = this.fieldName.lastIndexOf('.');
            String className = this.fieldName.substring(0, lastDotIndex);
            String pureClassName = JavaUtil.getSimpleClassName(className);
            String simpleFieldName = this.fieldName.substring(lastDotIndex + 1);
            this.veryShortEventString = "FIELDVAL: " + pureClassName + "." + simpleFieldName;
        }
        return this.veryShortEventString;
    }

    protected Object readResolve() {
        return (FieldValueTransition) Transition.getTransition(this);
    }
}
