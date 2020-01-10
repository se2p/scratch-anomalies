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
package newanalytics.bugpattern;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import newanalytics.IssueFinder;
import newanalytics.IssueReport;
import scratch.ast.model.ASTNode;
import scratch.ast.model.ActorDefinition;
import scratch.ast.model.Program;
import scratch.ast.model.event.StartedAsClone;
import scratch.ast.model.statement.common.CreateCloneOf;
import scratch.ast.model.variable.StrId;
import scratch.ast.visitor.ScratchVisitor;
import utils.Preconditions;

public class MissingCloneCall implements IssueFinder, ScratchVisitor {
    public static final String NAME = "missing_clone_call";
    public static final String SHORT_NAME = "mssCloneCll";
    private static final String NOTE1 = "There are no sprites with missing clone calls in your project.";
    private static final String NOTE2 = "Some of the sprites contain missing clone calls.";
    private List<String> whenStartsAsCloneActors = new ArrayList<>();
    private List<String> clonedActors = new ArrayList<>();
    private ActorDefinition currentActor;

    @Override
    public IssueReport check(Program program) {
        Preconditions.checkNotNull(program);
        whenStartsAsCloneActors = new ArrayList<>();
        clonedActors = new ArrayList<>();
        program.accept(this);
        final List<String> uninitializingActors
                = whenStartsAsCloneActors.stream().filter(s -> !clonedActors.contains(s)).collect(Collectors.toList());

        return new IssueReport(NAME, uninitializingActors.size(), uninitializingActors, "");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void visit(ActorDefinition actor) {
        currentActor = actor;
        if (!actor.getChildren().isEmpty()) {
            for (ASTNode child : actor.getChildren()) {
                child.accept(this);
            }
        }
    }

    @Override
    public void visit(CreateCloneOf node) {
        if (node.getStringExpr() instanceof StrId) {
            final String spriteName = ((StrId) node.getStringExpr()).getName();
            if (spriteName.equals("_myself_")) {
                clonedActors.add(currentActor.getIdent().getName());
            } else {
                clonedActors.add(spriteName);
            }
        }
    }

    @Override
    public void visit(StartedAsClone node) {
        whenStartsAsCloneActors.add(currentActor.getIdent().getName());
    }
}
