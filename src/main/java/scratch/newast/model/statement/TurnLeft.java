package scratch.newast.model.statement;

import scratch.newast.model.expression.numexpression.NumExpr;

public class TurnLeft implements SpriteMotionStmt {
    private NumExpr degrees;

    public TurnLeft(NumExpr degrees) {
        this.degrees = degrees;
    }

    public NumExpr getDegrees() {
        return degrees;
    }

    public void setDegrees(NumExpr degrees) {
        this.degrees = degrees;
    }
}