package org.softevo.oumextractor.modelcreator1;

import org.softevo.oumextractor.analysis.ClonesCache;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;

import java.util.Set;

/**
 * This class representes 'null' instances.
 *
 * @author Andrzej Wasylkowski
 */

/**
 * @author wasylkowski
 */
final class NullValue extends AValue {

    /**
     * The only instance of this class.
     */
    private static final NullValue INSTANCE = new NullValue();

    /**
     * Creates new representation of 'null' instance.
     */
    private NullValue() {
    }

    /**
     * Returns representation of 'null' instance.
     *
     * @return Representation of 'null' instance.
     */
    public static NullValue getInstance() {
        return NullValue.INSTANCE;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof NullValue) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 8;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#getSize()
     */
    @Override
    public int getSize() {
        return 1;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#canBeNonNull()
     */
    @Override
    public boolean canBeNonNull() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#canBeNull()
     */
    @Override
    public boolean canBeNull() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getArrayValues(boolean, java.util.Set)
     */
    @Override
    public void getArrayValues(boolean recursive, Set<ArrayValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getDoubleValues(java.util.Set)
     */
    @Override
    public void getDoubleValues(Set<DoubleValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getFloatValues(java.util.Set)
     */
    @Override
    public void getFloatValues(Set<FloatValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getIntegerValues(java.util.Set)
     */
    @Override
    public void getIntegerValues(Set<IntegerValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getLongValues(java.util.Set)
     */
    @Override
    public void getLongValues(Set<LongValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getModelableValues(java.util.Set)
     */
    @Override
    public void getModelableValues(Set<ModelableAValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getModelableRepresentations(java.util.Set)
     */
    @Override
    public void getModelableRepresentations(Set<ModelableAValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getObjectValues(java.util.Set)
     */
    @Override
    public void getObjectValues(Set<ObjectValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getObjectRepresentations(java.util.Set)
     */
    @Override
    public void getObjectRepresentations(Set<ObjectValue> result) {
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#merge(org.softevo.oumextractor.analysis.Value, org.softevo.oumextractor.analysis.MergesCache)
     */
    @Override
    public AValue merge(Value in_value, MergesCache merges) {
        AValue value = (AValue) in_value;
        AValue merge;

        if (merges.get(this, value) != null) {
            return (AValue) merges.get(this, value);
        }

        if (value instanceof NullValue) {
            merge = value;
        } else if (value instanceof MultipleValue) {
            merge = value.merge(this, merges);
        } else {
            merge = new MultipleValue(this, value);
        }

        merges.put(this, value, merge);
        return merge;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#clone(org.softevo.oumextractor.analysis.ClonesCache)
     */
    @Override
    public NullValue clone(ClonesCache clones) throws CloneNotSupportedException {
        if (clones.get(this) == null) {
            // there is always only one instance of NullValue
            clones.put(this, this);
        }
        return (NullValue) clones.get(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#updateStructure()
     */
    @Override
    public NullValue updateStructure() {
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "null";
    }
}
