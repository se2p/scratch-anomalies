/*
 * Copyright (C) 2019 LitterBox contributors
 *
 * This file is part of LitterBox.
 *
 * LitterBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * LitterBox is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LitterBox. If not, see <http://www.gnu.org/licenses/>.
 */
package scratch.ast.model.variable;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;
import scratch.ast.model.ASTNode;
import scratch.ast.visitor.ScratchVisitor;

public class Qualified implements Variable {

    private final Identifier first;
    private final Identifier second;
    private final ImmutableList<ASTNode> children;

    public Qualified(Identifier first, Identifier second) {
        this.first = Preconditions.checkNotNull(first);
        this.second = Preconditions.checkNotNull(second);
        children =  ImmutableList.<ASTNode>builder().add(first).add(second).build();
    }

    public Identifier getFirst() {
        return first;
    }

    public Identifier getSecond() {
        return second;
    }

    @Override
    public void accept(ScratchVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ImmutableList<ASTNode> getChildren() {
        return children;
    }
}