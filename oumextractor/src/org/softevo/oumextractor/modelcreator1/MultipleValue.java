package org.softevo.oumextractor.modelcreator1;

import org.softevo.oumextractor.analysis.ClonesCache;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to represent multiple values of many types.  The one
 * restriction is that values of the same type that are not modeled can't be
 * represented as a multiple value but rather as one value.
 *
 * @author Andrzej Wasylkowski
 */
final class MultipleValue extends AValue {

    /**
     * Set of values that constitute this multiple value.
     */
    private Set<AValue> values;

    /**
     * Creates new multiple value out of given two values. This constructor should
     * be called only if the two values given are not of the same type or if
     * they are modeled. No argument may be of 'MultipleValue' type.
     *
     * @param value1 One of values to constitute multiple value.
     * @param value2 One of values to constitute multiple value.
     */
    public MultipleValue(AValue value1, AValue value2) {
        if (value1 instanceof MultipleValue) {
            throw new IllegalArgumentException();
        } else if (value2 instanceof MultipleValue) {
            throw new IllegalArgumentException();
        } else if (value1.getClass().equals(value2.getClass()) &&
                !(value1 instanceof ModelableAValue)) {
            throw new IllegalArgumentException();
        } else {
            this.values = new HashSet<AValue>();
            this.values.add(value1);
            this.values.add(value2);
        }
    }

    /**
     * Creates new multiple value representing the same set of values as given
     * multiple value.
     *
     * @param other Reference multiple value.
     */
    public MultipleValue(MultipleValue other) {
        this.values = new HashSet<AValue>(other.values);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MultipleValue) {
            MultipleValue other = (MultipleValue) o;
            if (other.values.equals(this.values)) {
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
        int result = 7;
        result = 37 * result + this.values.hashCode();
        return result;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#getSize()
     */
    @Override
    public int getSize() {
        return this.values.iterator().next().getSize();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#canBeNonNull()
     */
    @Override
    public boolean canBeNonNull() {
        for (AValue value : this.values) {
            if (value.canBeNonNull()) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#canBeNull()
     */
    @Override
    public boolean canBeNull() {
        for (AValue value : this.values) {
            if (value.canBeNull()) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getArrayValues(boolean, java.util.Set)
     */
    @Override
    public void getArrayValues(boolean recursive, Set<ArrayValue> result) {
        for (AValue value : this.values) {
            value.getArrayValues(recursive, result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getDoubleValues(java.util.Set)
     */
    @Override
    public void getDoubleValues(Set<DoubleValue> result) {
        for (AValue value : this.values) {
            value.getDoubleValues(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getFloatValues(java.util.Set)
     */
    @Override
    public void getFloatValues(Set<FloatValue> result) {
        for (AValue value : this.values) {
            value.getFloatValues(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getIntegerValues(java.util.Set)
     */
    @Override
    public void getIntegerValues(Set<IntegerValue> result) {
        for (AValue value : this.values) {
            value.getIntegerValues(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getLongValues(java.util.Set)
     */
    @Override
    public void getLongValues(Set<LongValue> result) {
        for (AValue value : this.values) {
            value.getLongValues(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getModelableValues(java.util.Set)
     */
    @Override
    public void getModelableValues(Set<ModelableAValue> result) {
        for (AValue value : this.values) {
            value.getModelableValues(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getModelableRepresentations(java.util.Set)
     */
    @Override
    public void getModelableRepresentations(Set<ModelableAValue> result) {
        for (AValue value : this.values) {
            value.getModelableRepresentations(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getObjectValues(java.util.Set)
     */
    @Override
    public void getObjectValues(Set<ObjectValue> result) {
        for (AValue value : this.values) {
            value.getObjectValues(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getObjectRepresentations(java.util.Set)
     */
    @Override
    public void getObjectRepresentations(Set<ObjectValue> result) {
        for (AValue value : this.values) {
            value.getObjectRepresentations(result);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#merge(org.softevo.oumextractor.analysis.Value, org.softevo.oumextractor.analysis.MergesCache)
     */
    @Override
    public MultipleValue merge(Value in_value, MergesCache merges) {
        AValue value = (AValue) in_value;
        MultipleValue merge;

        if (merges.get(this, value) != null) {
            return (MultipleValue) merges.get(this, value);
        }

        // deal with merging with another multiple value
        if (value instanceof MultipleValue) {
            MultipleValue result = (MultipleValue) value;
            for (AValue v : this.values) {
                result = result.merge(v, merges);
            }
            if (result == value) {    // this suffices thanks to contract of 'merge'
                merge = result;
            } else if (result.equals(this)) {
                merge = this;
            } else {
                merge = result;
            }

            merges.put(this, value, merge);
            return merge;
        }

        // deal with merging with a single value
        for (AValue v : this.values) {
            if (v.getClass().equals(value.getClass())) {
                AValue mergeResult = value.merge(v, merges);
                if (mergeResult == v) {
                    merge = this;
                } else if (mergeResult instanceof MultipleValue) {
                    continue;
                } else {
                    MultipleValue result = new MultipleValue(this);
                    result.values.remove(v);
                    result.values.add(mergeResult);
                    merge = result;
                }
                merges.put(this, value, merge);
                return merge;
            }
        }

        MultipleValue result = new MultipleValue(this);
        result.values.add(value);

        merge = result;
        merges.put(this, value, merge);
        return merge;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#clone(org.softevo.oumextractor.analysis.ClonesCache)
     */
    @Override
    public MultipleValue clone(ClonesCache clones)
            throws CloneNotSupportedException {
        if (clones.get(this) == null) {
            MultipleValue result = (MultipleValue) super.clone();
            Set<AValue> newValues = new HashSet<AValue>();
            for (AValue v : result.values) {
                newValues.add(v.clone(clones));
            }
            result.values = newValues;
            clones.put(this, result);
        }
        return (MultipleValue) clones.get(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#updateStructure()
     */
    @Override
    public AValue updateStructure() {
        // update recursively, removing duplicates throughout
        Set<AValue> oldValues = new HashSet<AValue>(this.values);
        this.values.clear();
        for (AValue value : oldValues) {
            this.values.add(value.updateStructure());
        }

        // if there is only one element left, return it; otherwise return this
        if (this.values.size() == 1) {
            return this.values.iterator().next();
        } else {
            return this;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("MV [");
        for (AValue value : this.values) {
            result.append(value.toString()).append(", ");
        }
        result.delete(result.length() - 2, result.length());
        result.append("]");
        return result.toString();
    }
}
