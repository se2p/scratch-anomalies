package org.softevo.oumextractor.modelcreator1;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.softevo.oumextractor.analysis.ClonesCache;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;

import java.util.Set;

/**
 * This class is used to represent values being instances of types.
 *
 * @author Andrzej Wasylkowski
 */
final class ObjectValue extends ModelableAValue {

    /**
     * Indicates, whether this object is of the exact type kept as its type.
     */
    private boolean exactType;

    /**
     * Creates new representation of an instance of given type and puts it into
     * the set of all existing object values.
     *
     * @param typeName  Fully qualified name of a type.
     * @param exactType Indicates, if represented object is of an exact type
     *                  given or if it can be its subtype.
     */
    public ObjectValue(String typeName, boolean exactType) {
        super(typeName);
        this.exactType = exactType;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ObjectValue) {
            ObjectValue other = (ObjectValue) o;

            if (modelwiseEquals(other)) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // the hash code is selected so that it will not change during the
        // lifetime of the object
        return new HashCodeBuilder(9, 37).
                appendSuper(super.hashCode()).
                toHashCode();
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
        return true;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#canBeNull()
     */
    @Override
    public boolean canBeNull() {
        return false;
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
        result.add(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getModelableRepresentations(java.util.Set)
     */
    @Override
    public void getModelableRepresentations(Set<ModelableAValue> result) {
        result.add(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getObjectValues(java.util.Set)
     */
    @Override
    public void getObjectValues(Set<ObjectValue> result) {
        result.add(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getObjectRepresentations(java.util.Set)
     */
    @Override
    public void getObjectRepresentations(Set<ObjectValue> result) {
        result.add(this);
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

        if (value instanceof ObjectValue) {
            ObjectValue other = (ObjectValue) value;
            merge = modelwiseMerge(other);
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
    public ObjectValue clone(ClonesCache clones)
            throws CloneNotSupportedException {
        if (clones.get(this) == null) {
            ObjectValue result = (ObjectValue) super.clone();
            postClone(result);
            clones.put(this, result);
        }
        return (ObjectValue) clones.get(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#updateStructure()
     */
    @Override
    public ObjectValue updateStructure() {
        return this;
    }

    /**
     * Returns indicator, whether the type of this object is exact or just
     * a base type of a possible type.
     *
     * @return <code>true</code> if the type of this object is exactly as
     * specified by its type; <code>false</code> if the type can be
     * a subtype of the specified type.
     */
    public boolean isTypeExact() {
        return this.exactType;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).
                appendSuper(super.toString()).
                toString();
    }
}
