package org.softevo.oumextractor.modelcreator1.model;

import java.io.Serializable;

/**
 * This class is used to represent states of object usage models.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class State implements Serializable {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = 5626655050610798772L;

    /**
     * Unique id of the state.
     */
    protected final int id;

    /**
     * Constructs new state with given id.
     *
     * @param id Id of the state.
     */
    protected State(int id) {
        this.id = id;
    }

    /**
     * Returns the id of this state.
     *
     * @return The id of this state.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns 'dot' representation of this state.
     *
     * @return 'dot' representation of this state.
     */
    public abstract String getDotString();
}
