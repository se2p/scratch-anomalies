package org.softevo.oumextractor.analysis;

import java.util.*;

/**
 * This class is used for representations of a frame of a method.
 *
 * @author Andrzej Wasylkowski
 */
public final class Frame {

    /**
     * Operands stack of this frame.
     */
    private final LinkedList<Value> operandsStack;

    /**
     * List of local variables of this frame.
     */
    private final List<Value> localVariables;

    /**
     * Set of values that were in the frame before the node was analyzed.
     */
    private final IdentityHashMap<Value, Value> enteringValues;

    /**
     * Creates new empty frame with given number of slots for local variables.
     *
     * @param numSlots Number of slots for local variables.
     */
    public Frame(int numSlots) {
        this.operandsStack = new LinkedList<Value>();
        this.localVariables = new ArrayList<Value>();
        this.localVariables.addAll(Collections.nCopies(numSlots, (Value) null));
        this.enteringValues = new IdentityHashMap<Value, Value>();
    }

    /**
     * Creates new frame with contents copied (deep) from given frame.  Any two
     * identical objects remain identical (though different from original ones)
     * after copying.
     *
     * @param in Source frame.
     */
    public Frame(Frame in) {
        // make a shallow copy
        this.operandsStack = new LinkedList<Value>(in.operandsStack);
        this.localVariables = new ArrayList<Value>(in.localVariables);
        this.enteringValues = new IdentityHashMap<Value, Value>();
        ClonesCache clones = new ClonesCache();

        // clone values
        try {
            for (int i = 0; i < in.operandsStack.size(); i++) {
                if (in.operandsStack.get(i) == null) {
                    this.operandsStack.set(i, null);
                } else {
                    this.operandsStack.set(i,
                            in.operandsStack.get(i).clone(clones));
                }
            }
            for (int i = 0; i < in.localVariables.size(); i++) {
                if (in.localVariables.get(i) == null) {
                    this.localVariables.set(i, null);
                } else {
                    this.localVariables.set(i,
                            in.localVariables.get(i).clone(clones));
                }
            }
            for (Value value : in.enteringValues.keySet()) {
                Value clone = value.clone(clones);
                this.enteringValues.put(clone, clone);
            }
        } catch (CloneNotSupportedException e) {
            System.err.println("[ERROR] This should never happen");
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Creates a deep copy of this frame.
     *
     * @return A deep copy of this frame.
     */
    public Frame copy() {
        return new Frame(this);
    }

    /**
     * Pops an operand from the operands' stack.
     *
     * @return Operand at the top of the stack.
     */
    public Value popOperand() {
        return this.operandsStack.removeLast();
    }

    /**
     * Pushes an operand into the operands' stack.
     *
     * @param value Operand to be pushed.
     */
    public void pushOperand(Value value) {
        this.operandsStack.addLast(value);
    }

    /**
     * Clears the operands' stack.
     */
    public void clearOperandsStack() {
        this.operandsStack.clear();
    }

    /**
     * Initializes the set of entering values.
     */
    public void initializeEnteringValues() {
        this.enteringValues.clear();
        for (Value value : this.localVariables) {
            if (value != null) {
                this.enteringValues.put(value, value);
            }
        }
        for (Value value : this.operandsStack) {
            if (value != null) {
                this.enteringValues.put(value, value);
            }
        }
    }

    /**
     * Returns values that entered the frame, but disappeared afterwards.
     *
     * @return Values that entered the frame, but disappeared afterwards.
     */
    public Set<Value> getDisappearedValues() {
        IdentityHashMap<Value, Value> disappeared =
                new IdentityHashMap<Value, Value>(this.enteringValues);
        for (Value value : this.localVariables) {
            if (value != null) {
                disappeared.remove(value);
            }
        }
        for (Value value : this.operandsStack) {
            if (value != null) {
                disappeared.remove(value);
            }
        }
        return disappeared.keySet();
    }

    /**
     * Retains in the frame only variables from the given set.
     *
     * @param liveVariables Indices of variables to be retained.
     */
    public void retainVariables(Set<Integer> liveVariables) {
        for (int var = 0; var < this.localVariables.size(); var++) {
            if (!liveVariables.contains(var)) {
                setLocalVariable(var, null);
            }
        }
    }

    /**
     * Returns list of all values contained in an operand stack and in local
     * variables of this frame.
     *
     * @return List of values from operand stack and local variables.
     */
    public List<Value> getFrameValues() {
        List<Value> result = new ArrayList<Value>();
        for (Value value : this.localVariables) {
            if (value != null) {
                result.add(value);
            }
        }
        for (Value value : this.operandsStack) {
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Merges two frames into one.  This method must return its argument if the
     * result of the merging operation is equal to this argument.
     *
     * @param frame Frame to merge this frame with.
     * @return Result of merging this frame and the given one.
     */
    public Frame merge(Frame frame) {
        // if merging against non-existing frame, just return copy of this frame
        if (frame == null) {
            return new Frame(this);
        }

        // initialize merging
        boolean changed = false;
        Frame result = new Frame(frame);
        MergesCache merges = new MergesCache();

        // make sure frames are size-identical
        if (this.operandsStack.size() != frame.operandsStack.size() ||
                this.localVariables.size() != frame.localVariables.size()) {
            System.err.println("[ERROR] This should never happen");
            throw new IllegalStateException();
        }

        // merge operands' stacks
        for (int i = 0; i < this.operandsStack.size(); i++) {
            Value thisArg = this.operandsStack.get(i);
            Value arg = frame.operandsStack.get(i);
            Value merged = thisArg.merge(arg, merges);
            if (arg != merged) {
                result.operandsStack.set(i, merged);
                changed = true;
            }
        }

        // merge local variables
        for (int i = 0; i < this.localVariables.size(); i++) {
            Value thisArg = this.localVariables.get(i);
            Value arg = frame.localVariables.get(i);
            Value merged;
            if (arg == null) {
                merged = thisArg;
            } else if (thisArg == null) {
                merged = arg;
            } else if ((thisArg instanceof ReturnAddressValue ||
                    arg instanceof ReturnAddressValue) &&
                    !thisArg.getClass().equals(arg.getClass())) {
                merged = null;
            } else {
                merged = thisArg.merge(arg, merges);
            }
            if (arg != merged) {
                result.localVariables.set(i, merged);
                changed = true;
            }
        }

        // return appropriate frame
        if (changed) {
            return result;
        } else {
            return frame;
        }
    }

    /**
     * Updates structure of values in this frame.
     */
    public void updateValuesStructure() {
        for (int i = 0; i < this.operandsStack.size(); i++) {
            Value value = this.operandsStack.get(i);
            if (value != null) {
                value = value.updateStructure();
            }
            this.operandsStack.set(i, value);
        }
        for (int i = 0; i < this.localVariables.size(); i++) {
            Value value = this.localVariables.get(i);
            if (value != null) {
                value = value.updateStructure();
            }
            this.localVariables.set(i, value);
        }
        IdentityHashMap<Value, Value> updatedEnteringValues = new IdentityHashMap<Value, Value>();
        for (Value value : this.enteringValues.keySet()) {
            Value updated = value.updateStructure();
            updatedEnteringValues.put(updated, updated);
        }
        this.enteringValues.clear();
        this.enteringValues.putAll(updatedEnteringValues);
    }

    /**
     * Returns the number of local variables in this frame.
     *
     * @return The number of local variables.
     */
    public int getNumLocalVariables() {
        return this.localVariables.size();
    }

    /**
     * Returns local variable having a specified index.
     *
     * @param index Index of a local variable to get.
     * @return Value of a local variable of specified index.
     */
    public Value getLocalVariable(int index) {
        return this.localVariables.get(index);
    }

    /**
     * Sets local variable at a specified index to specified value.
     *
     * @param var   Index of a local variable to set.
     * @param value New value of a local variable.
     */
    public void setLocalVariable(int index, Value value) {
        this.localVariables.set(index, value);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Frame:\n");
        result.append("======================================================================\n");
        result.append("Local variables:\n");
        result.append("----------------------------------------\n");
        for (int i = 0; i < this.localVariables.size(); i++) {
            if (this.localVariables.get(i) == null) {
                result.append(i + " : (uninitialized)");
            } else {
                result.append(i + " : " + this.localVariables.get(i).toString());
            }
            result.append('\n');
        }
        result.append("Operands stack:\n");
        result.append("----------------------------------------\n");
        for (int i = 0; i < this.operandsStack.size(); i++) {
            result.append(i + " : " + this.operandsStack.get(i).toString());
            result.append('\n');
        }
        result.append("======================================================================");
        return result.toString();
    }
}
