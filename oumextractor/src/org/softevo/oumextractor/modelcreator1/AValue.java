package org.softevo.oumextractor.modelcreator1;

import org.softevo.oumextractor.analysis.ClonesCache;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;

import java.util.HashSet;
import java.util.Set;

/**
 * This is the base class for representing values.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class AValue extends org.softevo.oumextractor.analysis.Value {

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public abstract boolean equals(Object o);

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public abstract int hashCode();

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#merge(org.softevo.oumextractor.analysis.Value, org.softevo.oumextractor.analysis.MergesCache)
     */
    @Override
    public abstract AValue merge(Value value, MergesCache merges);

    /**
     * Indicates, whether this value may represent <code>null</code> or not.
     *
     * @return <code>true</code> if this value may represent <code>null</code>;
     * <code>false</code> otherwise.
     */
    public abstract boolean canBeNull();

    /**
     * Indicates, whether this value does not represent <code>null</code>.
     *
     * @return <code>true</code> if this value does not represent
     * <code>null</code>; <code>false</code> if it may represent
     * <code>null</code>.
     */
    public abstract boolean canBeNonNull();

    /**
     * Returns all representations of this value that are representations of
     * ArrayValue.
     *
     * @param recursive Indicates, whether elements of arrays that are arrays
     *                  themselves should be included or not
     * @return Set of representations of this value that are representations
     * of ArrayValue.
     */
    public final Set<ArrayValue> getArrayValues(boolean recursive) {
        Set<ArrayValue> result = new HashSet<ArrayValue>();
        getArrayValues(recursive, result);
        return result;
    }

    /**
     * Returns all elements of this value that are representations of
     * DoubleValue.
     *
     * @return Set of elements of this value that are representations
     * of DoubleValue.
     */
    public final Set<DoubleValue> getDoubleValues() {
        Set<DoubleValue> result = new HashSet<DoubleValue>();
        getDoubleValues(result);
        return result;
    }

    /**
     * Returns all elements of this value that are representations of
     * FloatValue.
     *
     * @return Set of elements of this value that are representations
     * of FloatValue.
     */
    public final Set<FloatValue> getFloatValues() {
        Set<FloatValue> result = new HashSet<FloatValue>();
        getFloatValues(result);
        return result;
    }

    /**
     * Returns all elements of this value that are representations of
     * IntegerValue.
     *
     * @return Set of elements of this value that are representations
     * of IntegerValue.
     */
    public final Set<IntegerValue> getIntegerValues() {
        Set<IntegerValue> result = new HashSet<IntegerValue>();
        getIntegerValues(result);
        return result;
    }

    /**
     * Returns all elements of this value that are representations of
     * LongValue.
     *
     * @return Set of elements of this value that are representations
     * of LongValue.
     */
    public final Set<LongValue> getLongValues() {
        Set<LongValue> result = new HashSet<LongValue>();
        getLongValues(result);
        return result;
    }

    /**
     * Returns all elements of this value that are representations of
     * ModelableAValue.
     *
     * @return Set of elements of this value that are representations
     * of ModelableAValue.
     */
    public final Set<ModelableAValue> getModelableValues() {
        Set<ModelableAValue> result = new HashSet<ModelableAValue>();
        getModelableValues(result);
        return result;
    }

    /**
     * Returns all representations of this value that are representations of
     * ModelableAValue.
     *
     * @return Set of representations of this value that are representations
     * of ModelableAValue.
     */
    public final Set<ModelableAValue> getModelableRepresentations() {
        Set<ModelableAValue> result = new HashSet<ModelableAValue>();
        getModelableRepresentations(result);
        return result;
    }

    /**
     * Returns all elements of this value that are representations of
     * ObjectValue.
     *
     * @return Set of elements of this value that are representations
     * of ObjectValue.
     */
    public final Set<ObjectValue> getObjectValues() {
        Set<ObjectValue> result = new HashSet<ObjectValue>();
        getObjectValues(result);
        return result;
    }

    /**
     * Returns all representations of this value that are representations of
     * ObjectValue.
     *
     * @return Set of representations of this value that are representations
     * of ObjectValue.
     */
    public final Set<ObjectValue> getObjectRepresentations() {
        Set<ObjectValue> result = new HashSet<ObjectValue>();
        getObjectRepresentations(result);
        return result;
    }

    /**
     * Puts all representations of this value that are representations of
     * ArrayValue into given collection.
     *
     * @param recursive Indicates, whether elements of arrays that are arrays
     *                  themselves should be included or not
     * @param result    Collection to put array values to.
     */
    public abstract void getArrayValues(boolean recursive,
                                        Set<ArrayValue> result);

    /**
     * Puts all representations of this value that are representations of
     * DoubleValue into given collection.
     *
     * @param result Collection to put double values to.
     */
    public abstract void getDoubleValues(Set<DoubleValue> result);

    /**
     * Puts all representations of this value that are representations of
     * FloatValue into given collection.
     *
     * @param result Collection to put float values to.
     */
    public abstract void getFloatValues(Set<FloatValue> result);

    /**
     * Puts all representations of this value that are representations of
     * IntegerValue into given collection.
     *
     * @param result Collection to put integer values to.
     */
    public abstract void getIntegerValues(Set<IntegerValue> result);

    /**
     * Puts all representations of this value that are representations of
     * LongValue into given collection.
     *
     * @param result Collection to put long values to.
     */
    public abstract void getLongValues(Set<LongValue> result);

    /**
     * Puts all elements of this value that are representations of
     * ModelableAValue into given collection.
     *
     * @param result Collection to put modelable values to.
     */
    public abstract void getModelableValues(Set<ModelableAValue> result);

    /**
     * Puts all representations of this value that are representations of
     * ModelableAValue into given collection.
     *
     * @param result Collection to put modelable values to.
     */
    public abstract void getModelableRepresentations(Set<ModelableAValue> result);

    /**
     * Puts all elements of this value that are representations of
     * ObjectValue into given collection.
     *
     * @param result Collection to put object values to.
     */
    public abstract void getObjectValues(Set<ObjectValue> result);

    /**
     * Puts all representations of this value that are representations of
     * ObjectValue into given collection.
     *
     * @param result Collection to put object values to.
     */
    public abstract void getObjectRepresentations(Set<ObjectValue> result);

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#clone(org.softevo.oumextractor.analysis.ClonesCache)
     */
    @Override
    public abstract AValue clone(ClonesCache clones)
            throws CloneNotSupportedException;

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#updateStructure()
     */
    @Override
    public abstract AValue updateStructure();
}
