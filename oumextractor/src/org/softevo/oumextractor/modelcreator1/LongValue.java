package org.softevo.oumextractor.modelcreator1;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectweb.asm.Opcodes;
import org.softevo.oumextractor.analysis.ClonesCache;
import org.softevo.oumextractor.analysis.MergesCache;
import org.softevo.oumextractor.analysis.Value;

import java.util.Set;

/**
 * This class is used to represent long values.
 *
 * @author Andrzej Wasylkowski
 */
final class LongValue extends AValue {

    /**
     * This is the one and only instance of a long value.
     */
    private static final LongValue INSTANCE = new LongValue();

    /**
     * Creates new representation of any long value.
     */
    private LongValue() {
        assert INSTANCE == null;
    }

    /**
     * Returns the one and only instance of a long value.
     *
     * @return The one and only instance of a long value.
     */
    public static LongValue getInstance() {
        return INSTANCE;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof LongValue) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(61, 37).
                toHashCode();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#getSize()
     */
    @Override
    public int getSize() {
        return 2;
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
        result.add(this);
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

        if (value instanceof LongValue) {
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
    public LongValue clone(ClonesCache clones) throws CloneNotSupportedException {
        if (clones.get(this) == null) {
            LongValue result = INSTANCE;
            clones.put(this, result);
        }
        return (LongValue) clones.get(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.modelcreator1.AValue#updateStructure()
     */
    @Override
    public LongValue updateStructure() {
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
            case Opcodes.L2D:
                return DoubleValue.getInstance();

            case Opcodes.L2F:
                return FloatValue.getInstance();

            case Opcodes.L2I:
                return IntegerValue.getInstance();

            case Opcodes.LNEG:
                return INSTANCE;

            default:
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException("opcode = " + opcode);
        }
    }

    /**
     * Returns result of performing a commutative binary operation of given
     * opcode on this value and given argument.
     *
     * @param opcode Opcode of operation to perform.
     * @param arg    Second argument of the operation.
     * @return Result on performing given operation on this value and the argument.
     */
    public AValue performCommutativeOperation(int opcode, AValue arg) {
        switch (opcode) {
            case Opcodes.LADD:
                return INSTANCE;

            case Opcodes.LAND:
                return INSTANCE;

            case Opcodes.LMUL:
                return INSTANCE;

            case Opcodes.LOR:
                return INSTANCE;

            case Opcodes.LXOR:
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
     * @return Result on performing given operation on this value and the argument.
     */
    public AValue performNonCommutativeOperationLeft(int opcode, AValue arg) {
        switch (opcode) {
            case Opcodes.LDIV:
                return INSTANCE;

            case Opcodes.LREM:
                return INSTANCE;

            case Opcodes.LSHL:
                return INSTANCE;

            case Opcodes.LSHR:
                return INSTANCE;

            case Opcodes.LSUB:
                return INSTANCE;

            case Opcodes.LUSHR:
                return INSTANCE;

            case Opcodes.LCMP:
                return IntegerValue.getInstance();

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
     * @return Result on performing given operation on this value and the argument.
     */
    public AValue performNonCommutativeOperationRight(int opcode, AValue arg) {
        switch (opcode) {
            case Opcodes.LDIV:
                return INSTANCE;

            case Opcodes.LREM:
                return INSTANCE;

            case Opcodes.LSUB:
                return INSTANCE;

            case Opcodes.LCMP:
                return IntegerValue.getInstance();

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
