package de.uni_passau.fim.se2.litterbox.ast.model.statement.actorsound;

import de.uni_passau.fim.se2.litterbox.ast.model.AbstractNode;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.num.NumExpr;
import de.uni_passau.fim.se2.litterbox.ast.model.metadata.block.BlockMetadata;
import de.uni_passau.fim.se2.litterbox.ast.visitor.ScratchVisitor;

public class ChangeSoundEffectBy extends AbstractNode implements ActorSoundStmt {
    private final NumExpr value;
    private final SoundEffect effect;
    private final BlockMetadata metadata;

    public ChangeSoundEffectBy(SoundEffect effect, NumExpr value, BlockMetadata metadata) {
        super(effect, value, metadata);
        this.value = value;
        this.effect = effect;
        this.metadata=metadata;
    }

    public BlockMetadata getMetadata() {
        return metadata;
    }

    public NumExpr getValue() {
        return value;
    }

    public SoundEffect getEffect() {
        return effect;
    }

    @Override
    public void accept(ScratchVisitor visitor) {
        visitor.visit(this);
    }
}
