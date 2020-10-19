package org.softevo.oumextractor.modelcreator1.model;

/**
 * This class is used to represent states associated with abnormal exits from a
 * method, that is exits caused by unhandled exceptions.
 *
 * @author Andrzej Wasylkowski
 */
final class AbnormalExitState extends State {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 103307009621400011L;

    /**
     * Fully qualified name of an exception class.
     */
    private final String excType;

    /**
     * Creates new abnormal exit state associated with given exception.
     *
     * @param id      Id of this state.
     * @param excType Fully qualified name of an exception class.
     */
    public AbnormalExitState(int id, String excType) {
        super(id);
        this.excType = excType;
    }

    /**
     * Returns fully qualified name of an exception class.
     *
     * @return Fully qualified name of an exception class.
     */
    public String getExceptionType() {
        return this.excType;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.State#getDotString()
     */
    @Override
    public String getDotString() {
        StringBuffer result = new StringBuffer();
        result.append("\"").append("EXIT").append(" : ").append(excType).append("\"");
        return result.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EXIT : " + excType;
    }
}
