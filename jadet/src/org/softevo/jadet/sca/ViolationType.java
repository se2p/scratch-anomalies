package org.softevo.jadet.sca;


/**
 * Elements of this enumeration represent violation types.
 *
 * @author Andrzej Wasylkowski
 */
public enum ViolationType {

    /**
     * Indicates a violation that has not been classified yet.
     */
    UNKNOWN(false, "(unknown)"),

    /**
     * Indicates a violation that is a false positive.
     */
    FALSE_POSITIVE(false, "false positive"),

    /**
     * Indicates a violation that is a hint.
     */
    HINT(true, "hint"),

    /**
     * Indicates a violation that is a code smell.
     */
    CODE_SMELL(true, "code smell"),

    /**
     * Indicates a violation that is a defect.
     */
    DEFECT(true, "defect");


    /**
     * Indicates, if the violation is a true positive or not.
     */
    private final boolean truePositive;


    /**
     * Human readable string representation of the violation type.
     */
    private final String stringRepresentation;


    /**
     * Creates a new violation type.
     *
     * @param truePositive         Indicates, if the violation is a true
     *                             positive.
     * @param stringRepresentation Human readable representation of this
     *                             violation type.
     */
    ViolationType(boolean truePositive, String stringRepresentation) {
        this.truePositive = truePositive;
        this.stringRepresentation = stringRepresentation;
    }


    /**
     * Returns <code>true</code>, if this violation type is a true positive.
     *
     * @return    <code>true</code>, if this violation type is a true positive.
     */
    public boolean isTruePositive() {
        return this.truePositive;
    }


    /**
     * Returns human readable representation of this violation type.
     *
     * @return Human readable representation of this violation type.
     */
    public String getStringRepresentation() {
        return this.stringRepresentation;
    }
}
