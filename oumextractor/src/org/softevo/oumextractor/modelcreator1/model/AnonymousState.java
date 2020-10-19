package org.softevo.oumextractor.modelcreator1.model;

/**
 * This class is used to represent anonymous states of object usage models.
 *
 * @author Andrzej Wasylkowski
 */
final class AnonymousState extends State {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -5873970308555590012L;

    /**
     * Creates new anonymous state with given id.
     *
     * @param id Id of state.
     */
    public AnonymousState(int id) {
        super(id);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.model.State#getDotString()
     */
    @Override
    public String getDotString() {
        StringBuffer result = new StringBuffer();
        result.append("\"").append(id).append("\"");
        return result.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "AS " + this.id;
    }
}
