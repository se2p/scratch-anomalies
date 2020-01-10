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

import java.util.LinkedList;
import java.util.List;
import newanalytics.IssueFinder;
import newanalytics.IssueReport;
import scratch.ast.Constants;
import scratch.ast.model.ASTNode;
import scratch.ast.model.ActorDefinition;
import scratch.ast.model.Program;
import scratch.ast.model.procedure.Parameter;
import scratch.ast.model.procedure.ProcedureDefinition;
import scratch.ast.model.variable.StrId;
import scratch.ast.visitor.ScratchVisitor;
import utils.Preconditions;

public class OrphanedParameter implements IssueFinder, ScratchVisitor {
    private static final String NOTE1 = "There are no orphaned parameters in your project.";
    private static final String NOTE2 = "Some of the procedures contain orphaned parameters.";
    public static final String NAME = "orphaned_parameter";
    public static final String SHORT_NAME = "OrphParam";
    private boolean found = false;
    private int count = 0;
    private List<String> actorNames = new LinkedList<>();
    private ActorDefinition currentActor;
    private List<Parameter> currentParameters;
    private boolean insideProcedure;

    @Override
    public IssueReport check(Program program) {
        Preconditions.checkNotNull(program);
        found = false;
        count = 0;
        actorNames = new LinkedList<>();
        program.accept(this);
        String notes = NOTE1;
        if (count > 0) {
            notes = NOTE2;
        }
        return new IssueReport(NAME, count, actorNames, notes);
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

        if (found) {
            found = false;
            actorNames.add(currentActor.getIdent().getName());
        }
    }

    @Override
    public void visit(ProcedureDefinition node) {
        insideProcedure = true;
        currentParameters = node.getParameterList().getParameterListPlain().getParameters();
        if (!node.getChildren().isEmpty()) {
            for (ASTNode child : node.getChildren()) {
                child.accept(this);
            }
        }
        insideProcedure = false;
    }

    @Override
    public void visit(StrId node) {
        if (insideProcedure) {
            if (node.getName().startsWith(Constants.PARAMETER_ABBREVIATION)) {
                checkParameterNames(node.getName());
            }
        }
        if (!node.getChildren().isEmpty()) {
            for (ASTNode child : node.getChildren()) {
                child.accept(this);
            }
        }

    }

    private void checkParameterNames(String name) {
        boolean validParametername = false;
        for (int i = 0; i < currentParameters.size() && !validParametername; i++) {
            if (name.equals(currentParameters.get(i).getIdent().getName())) {
                validParametername = true;
            }
        }
        if (!validParametername) {
            count++;
            found = true;
        }
    }
}
