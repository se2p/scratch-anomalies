/*
 * Copyright (C) 2020 LitterBox contributors
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
package de.uni_passau.fim.se2.litterbox.ast.model.statement.actorsound;

import de.uni_passau.fim.se2.litterbox.ast.model.ASTLeaf;
import de.uni_passau.fim.se2.litterbox.ast.model.ASTNode;
import de.uni_passau.fim.se2.litterbox.ast.visitor.ScratchVisitor;
import java.util.Collections;
import java.util.List;

public enum SoundEffect implements ASTLeaf {

    PAN("pan left/right"), PITCH("pitch");

    private final String token;

    SoundEffect(String token) {
        this.token = token;
    }

    public static boolean contains(String opcode) {
        for (SoundEffect value : SoundEffect.values()) {
            if (value.name().equals(opcode)) {
                return true;
            }
        }
        return false;
    }

    public String getToken() {
        return token;
    }

    public static SoundEffect fromString(String type) {
        for (SoundEffect f : values()) {
            if (f.getToken().startsWith(type.toLowerCase())) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown SoundEffect: " + type);
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
        result[0] = token;
        return result;
    }
}
