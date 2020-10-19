package org.softevo.oumextractor.analysis;

import org.softevo.oumextractor.controlflow.Node;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents return address values.
 *
 * @author Andrzej Wasylkowski
 */
public final class ReturnAddressValue extends Value {

    /**
     * Holds the nodes that represent the encapsulated return address.
     */
    private final Set<Node> retNodes;

    /**
     * Creates new return address value without any node encapsulated.
     */
    private ReturnAddressValue() {
        this.retNodes = new HashSet<Node>();
    }

    /**
     * Creates new return address value that holds given nodes.
     *
     * @param retNodes Nodes that represent the encapsulated return address.
     */
    private ReturnAddressValue(Set<Node> retNodes) {
        this.retNodes = new HashSet<Node>(retNodes);
    }

    /**
     * Creates new return address value that holds given node.
     *
     * @param retNode Node encapsulating return address to represent.
     */
    ReturnAddressValue(Node retNode) {
        this.retNodes = new HashSet<Node>();
        this.retNodes.add(retNode);
    }

    /**
     * Returns node that represent the encapsulated return address.
     *
     * @return Node that represents the encapsulated return address.
     */
    public Set<Node> getRetNodes() {
        return Collections.unmodifiableSet(this.retNodes);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#getSize()
     */
    @Override
    public int getSize() {
        return 1;
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#merge(org.softevo.oumextractor.analysis.Value, org.softevo.oumextractor.analysis.MergesCache)
     */
    @Override
    public Value merge(Value value, MergesCache merges) {
        if (merges.get(this, value) == null) {
            if (value instanceof ReturnAddressValue) {
                ReturnAddressValue other = (ReturnAddressValue) value;
                ReturnAddressValue merged;
                if (other.retNodes.containsAll(this.retNodes)) {
                    merged = other;
                } else if (this.retNodes.containsAll(other.retNodes)) {
                    merged = this;
                } else {
                    merged = new ReturnAddressValue();
                    merged.retNodes.addAll(this.retNodes);
                    merged.retNodes.addAll(other.retNodes);
                }
                merges.put(this, value, merged);
            } else {
                System.err.println("[ERROR] This should never happen");
                throw new IllegalStateException();
            }
        }

        return merges.get(this, value);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#clone(org.softevo.oumextractor.analysis.ClonesCache)
     */
    @Override
    public ReturnAddressValue clone(ClonesCache clones)
            throws CloneNotSupportedException {
        if (clones.get(this) == null) {
            ReturnAddressValue result = new ReturnAddressValue(this.retNodes);
            clones.put(this, result);
        }

        return (ReturnAddressValue) clones.get(this);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.analysis.Value#updateStructure()
     */
    @Override
    public ReturnAddressValue updateStructure() {
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + this.retNodes.hashCode();
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ReturnAddressValue) {
            ReturnAddressValue other = (ReturnAddressValue) o;
            return this.retNodes.equals(other.retNodes);
        }
        return false;
    }
}
