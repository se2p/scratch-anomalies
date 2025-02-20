package org.softevo.oumextractor.modelcreator1;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectweb.asm.Opcodes;
import org.softevo.oumextractor.analysis.ClonesCache;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;

import java.util.Set;

/**
 * This class is used to represent integer values.
 *
 * @author Andrzej Wasylkowski
 */
final class IntegerValue extends AValue {

    /**
     * The one and only instance of an integer value.
     */
    private static IntegerValue INSTANCE = new IntegerValue();

    /**
     * Creates new representation of any integer value.
     */
    private IntegerValue() {
        assert INSTANCE == null;
    }

    /**
     * Returns the one and only instance of an integer value.
     *
     * @return The one and only instance of an integer value.
     */
    public static IntegerValue getInstance() {
        return INSTANCE;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof IntegerValue) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 37).
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
        result.add(this);
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

        if (value instanceof IntegerValue) {
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
    public IntegerValue clone(ClonesCache clones)
            throws CloneNotSupportedException {
        if (clones.get(this) == null) {
            IntegerValue result = INSTANCE;
            clones.put(this, result);
        }
        return (IntegerValue) clones.get(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#updateStructure()
     */
    @Override
    public IntegerValue updateStructure() {
        return this;
    }

    /**
     * Returns result of performing an unary operation of given opcode on this value.
     *
     * @param opcode Opcode of operation to perform.
     * @return Result on performing given operation on this value.
     */
    public AValue performOperation(int opcode) {
        switch (opcode) {
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                // TODO: this needs to be perhaps changed a little bit by adding
                // a transition denoting casting or something like this
                return INSTANCE;

            case Opcodes.INEG:
                return INSTANCE;

            case Opcodes.I2D:
                return DoubleValue.getInstance();

            case Opcodes.I2F:
                return FloatValue.getInstance();

            case Opcodes.I2L:
                return LongValue.getInstance();

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " + opcode);
        }
    }

    /**
     * Returns result of performing an iinc operation with given increment on
     * this value.
     *
     * @param increment Increment used in the iinc operation.
     * @return Result of performing an increment operation on this value.
     */
    public AValue performIincOperation(int increment) {
        return INSTANCE;
    }

    /**
     * Returns result of performing a commutative binary operation of given
     * opcode on this value and given argument.
     *
     * @param opcode Opcode of operation to perform.
     * @param arg    Second argument of the operation.
     * @return Result of performing given operation on this value and the argument.
     */
    public AValue performCommutativeOperation(int opcode, AValue arg) {
        switch (opcode) {
            case Opcodes.IADD:
                return INSTANCE;

            case Opcodes.IAND:
                return INSTANCE;

            case Opcodes.IMUL:
                return INSTANCE;

            case Opcodes.IOR:
                return INSTANCE;

            case Opcodes.IXOR:
                return INSTANCE;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " + opcode);
        }
    }

    /**
     * Returns result of performing a noncommutative binary operation of given
     * opcode on this value as a left argument and given right argument.
     *
     * @param opcode Opcode of operation to perform.
     * @param arg    Second argument of the operation.
     * @return Result of performing given operation on this value and the argument.
     */
    public AValue performNonCommutativeOperationLeft(int opcode, AValue arg) {
        switch (opcode) {
            case Opcodes.IDIV:
                return INSTANCE;

            case Opcodes.IREM:
                return INSTANCE;

            case Opcodes.ISHL:
                return INSTANCE;

            case Opcodes.ISHR:
                return INSTANCE;

            case Opcodes.ISUB:
                return INSTANCE;

            case Opcodes.IUSHR:
                return INSTANCE;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " + opcode);
        }
    }

    /**
     * Returns result of performing a noncommutative binary operation of given
     * opcode on this value as a right argument and given left argument.
     *
     * @param opcode Opcode of operation to perform.
     * @param arg    Second argument of the operation.
     * @return Result of performing given operation on this value and the argument.
     */
    public AValue performNonCommutativeOperationRight(int opcode, AValue arg) {
        switch (opcode) {
            case Opcodes.IDIV:
                return INSTANCE;

            case Opcodes.IREM:
                return INSTANCE;

            case Opcodes.ISHL:
                return INSTANCE;

            case Opcodes.ISHR:
                return INSTANCE;

            case Opcodes.ISUB:
                return INSTANCE;

            case Opcodes.IUSHR:
                return INSTANCE;

            case Opcodes.LSHL:
                return INSTANCE;

            case Opcodes.LSHR:
                return INSTANCE;

            case Opcodes.LUSHR:
                return INSTANCE;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " + opcode);
        }
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
