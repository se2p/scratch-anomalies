package org.softevo.oumextractor.analysis;

import java.util.IdentityHashMap;

/**
 * This class is used to keep information about results of cloning values.
 *
 * @author Andrzej Wasylkowski
 */
public final class ClonesCache {

    /**
     * Mapping (value => clones).
     */
    private final IdentityHashMap<Value, Value> clones;

    /**
     * Creates new cache of cloning results.
     */
    public ClonesCache() {
        this.clones = new IdentityHashMap<Value, Value>();
    }

    /**
     * Returns cached result of cloning given value.
     *
     * @param value Cloned value.
     * @return Cached result of cloning given value or <code>null</code>.
     */
    public Value get(Value value) {
        return this.clones.get(value);
    }

    /**
     * Inserts result of cloning given value into the cache.
     *
     * @param value  Cloned value.
     * @param result Result of cloning the value.
     */
    public void put(Value value, Value result) {
        this.clones.put(value, result);
    }
}
