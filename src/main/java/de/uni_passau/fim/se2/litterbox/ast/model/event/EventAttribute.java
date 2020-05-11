package de.uni_passau.fim.se2.litterbox.ast.model.event;

import de.uni_passau.fim.se2.litterbox.ast.model.ASTLeaf;
import de.uni_passau.fim.se2.litterbox.ast.model.ASTNode;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.string.attributes.FixedAttribute;
import de.uni_passau.fim.se2.litterbox.ast.visitor.ScratchVisitor;
import de.uni_passau.fim.se2.litterbox.utils.Preconditions;

import java.util.Collections;
import java.util.List;

public enum EventAttribute implements ASTLeaf {
    LOUDNESS("loudness"), TIMER("timer");
    private final String type;

    EventAttribute(String type) {
        this.type = Preconditions.checkNotNull(type);
    }

    public static EventAttribute fromString(String type) {
        for (EventAttribute f : values()) {
            if (f.getType().equals(type.toLowerCase())) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown EventAttribute: " + type);
    }

    public String getType() {
        return type;
    }

    @Override
    public void accept(ScratchVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<? extends ASTNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public String getUniqueName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String[] toSimpleStringArray() {
        String[] result = new String[1];
        result[0] = type;
        return result;
    }
}