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
package scratch.ast.model.statement.declaration;

import com.google.common.collect.ImmutableList;
import scratch.ast.model.ASTNode;
import scratch.ast.model.type.Type;
import scratch.ast.model.variable.Identifier;
import scratch.ast.visitor.ScratchVisitor;

public class DeclarationIdentAsTypeStmt implements DeclarationStmt {

    private Identifier ident;
    private Type type;
    private final ImmutableList<ASTNode> children;

    public DeclarationIdentAsTypeStmt(Identifier ident, Type type) {
        this.ident = ident;
        this.type = type;
        ImmutableList.Builder<ASTNode> builder = ImmutableList.builder();
        children = builder.add(ident).add(type).build();
    }

    public Identifier getIdent() {
        return ident;
    }

    public void setIdent(Identifier ident) {
        this.ident = ident;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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