package org.softevo.oumextractor.modelcreator1;

import org.softevo.oumextractor.analysis.ClonesCache;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;

import java.util.Set;

/**
 * This class is used to represent arrays.
 *
 * @author Andrzej Wasylkowski
 */
final class ArrayValue extends AValue {

    /**
     * Representation of an element of this array.
     */
    private AValue element;

    /**
     * Representation of a size of this array.
     */
    private AValue size;

    /**
     * Creates new array value with given element as an element representation and
     * given size (number of elements).
     *
     * @param element Representation of an element of the array.
     * @param size    Size (number of elements) of the array.
     */
    public ArrayValue(AValue element, AValue size) {
        this.element = element;
        this.size = size;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ArrayValue) {
            ArrayValue other = (ArrayValue) o;
            if (other.element.equals(this.element) &&
                    other.size.equals(this.size)) {
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
        int result = 2;
        result = 37 * result + this.element.hashCode();
        result = 37 * result + this.size.hashCode();
        return result;
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
        result.add(this);
        if (recursive) {
            this.element.getArrayValues(recursive);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getFloatValues(java.util.Set)
     */
    @Override
    public void getFloatValues(Set<FloatValue> result) {
        this.element.getFloatValues(result);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getDoubleValues(java.util.Set)
     */
    @Override
    public void getDoubleValues(Set<DoubleValue> result) {
        this.element.getDoubleValues(result);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getIntegerValues(java.util.Set)
     */
    @Override
    public void getIntegerValues(Set<IntegerValue> result) {
        this.element.getIntegerValues(result);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getLongValues(java.util.Set)
     */
    @Override
    public void getLongValues(Set<LongValue> result) {
        this.element.getLongValues(result);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#getModelableValues(java.util.Set)
     */
    @Override
    public void getModelableValues(Set<ModelableAValue> result) {
        this.element.getModelableValues(result);
        this.size.getModelableValues(result);
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
        this.element.getObjectValues(result);
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

        if (value instanceof ArrayValue) {
            ArrayValue other = (ArrayValue) value;
            AValue newElement = this.element.merge(other.element, merges);
            AValue newSize = this.size.merge(other.size, merges);
            ArrayValue result = new ArrayValue(newElement, newSize);
            if (result.equals(value)) {
                merge = value;
            } else {
                merge = result;
            }
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
    public ArrayValue clone(ClonesCache clones) throws CloneNotSupportedException {
        if (clones.get(this) == null) {
            ArrayValue result = (ArrayValue) super.clone();
            result.element = result.element.clone(clones);
            result.size = result.size.clone(clones);
            clones.put(this, result);
        }

        return (ArrayValue) clones.get(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#updateStructure()
     */
    @Override
    public ArrayValue updateStructure() {
        this.element = this.element.updateStructure();
        this.size = this.size.updateStructure();
        return this;
    }

    /**
     * Returns size of this array (its number of elements).
     *
     * @return Size of this array.
     */
    public AValue getArraySize() {
        return this.size;
    }

    /**
     * Returns representation of an element of this array at a given index.
     *
     * @param index Index of an element, whose representation is to be returned.
     * @return Representation of an element of given index.
     */
    public AValue getArrayElement(AValue index) {
        return this.element;
    }

    /**
     * Sets representation of an element of this array at a given index to given
     * object.
     *
     * @param index   Index of an element to set.
     * @param element Representation of an element to store in this array.
     */
    public void setArrayElement(AValue index, AValue element) {
        this.element = element.merge(this.element, new MergesCache());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("AV (").append(this.size.toString()).append(", ");
        result.append(this.element.toString()).append(")");
        return result.toString();
    }
}
