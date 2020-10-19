package org.softevo.oumextractor.analysis;

/**
 * This class is used as a base class for representing Java bytecode values.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class Value implements Cloneable {

    /**
     * Returns size of this value (its "computational type" as described in JVM
     * specification).
     *
     * @return Size of this value.
     */
    public abstract int getSize();

    /**
     * Merges this value with the one given as a parameter.  If the result of
     * merging is equal to the parameter, the parameter itself must be returned.
     * If this is not the case, but the result of merging is equal to this object,
     * this object itself must be returned.  Otherwise new value representing both
     * this value and given parameter must be returned.
     *
     * @param value  Parameter to merge this value with.
     * @param merges Existing results of merges that should be taken into account.
     * @return Result of merging this value and a given parameter.
     */
    public abstract Value merge(Value value, MergesCache merges);

    /**
     * Clones this value.
     *
     * @param clones Existing results of cloning that should be taken into account.
     * @return Result of cloning this value.
     */
    public abstract Value clone(ClonesCache clones)
            throws CloneNotSupportedException;

    /**
     * Updates structure of this value.  This method is called on a value if
     * it (or its subvalues) may have underwent some significant change that
     * may warrant change of the structure of the value.
     */
    public abstract Value updateStructure();
}
