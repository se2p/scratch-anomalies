package org.softevo.oumextractor.analysis;

import java.util.IdentityHashMap;

/**
 * This class is used to keep information about results of merging values.
 *
 * @author Andrzej Wasylkowski
 */
public final class MergesCache {

    /**
     * Mapping (value => (mapping value => merge result).
     */
    private final IdentityHashMap<Value, IdentityHashMap<Value, Value>> merges;

    /**
     * Creates new cache of merging results.
     */
    public MergesCache() {
        this.merges = new IdentityHashMap<Value, IdentityHashMap<Value, Value>>();
    }

    /**
     * Returns cached result of merging given two values.
     *
     * @param value1 One of the values.
     * @param value2 One of the values.
     * @return Cached result of merging given two values or <code>null</code>.
     */
    public Value get(Value value1, Value value2) {
        if (this.merges.containsKey(value1)) {
            return this.merges.get(value1).get(value2);
        }
        return null;
    }

    /**
     * Inserts result of merging given two values into the cache.
     *
     * @param value1 One of the values.
     * @param value2 One of the values.
     * @param result Result of merging the two values.
     */
    public void put(Value value1, Value value2, Value result) {
        if (!this.merges.containsKey(value1)) {
            this.merges.put(value1, new IdentityHashMap<Value, Value>());
        }

        this.merges.get(value1).put(value2, result);
    }
}
