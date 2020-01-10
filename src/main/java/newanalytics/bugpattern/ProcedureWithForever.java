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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import newanalytics.IssueFinder;
import newanalytics.IssueReport;
import scratch.ast.model.ASTNode;
import scratch.ast.model.ActorDefinition;
import scratch.ast.model.Program;
import scratch.ast.model.StmtList;
import scratch.ast.model.procedure.ProcedureDefinition;
import scratch.ast.model.statement.CallStmt;
import scratch.ast.model.statement.Stmt;
import scratch.ast.model.statement.control.RepeatForeverStmt;
import scratch.ast.model.variable.Identifier;
import scratch.ast.parser.symboltable.ProcedureInfo;
import scratch.ast.visitor.ScratchVisitor;
import utils.Preconditions;

public class ProcedureWithForever implements IssueFinder, ScratchVisitor {
    public static final String NAME = "procedure_with_forever";
    public static final String SHORT_NAME = "procWithForever";
    private static final String NOTE1 = "There are no procedures with forever where the call is followed by statements in your project.";
    private static final String NOTE2 = "Some of the sprites contain procedures with forever where the call is followed by statements.";
    private boolean found = false;
    private int count = 0;
    private List<String> actorNames = new LinkedList<>();
    private ActorDefinition currentActor;
    private String currentProcedureName;
    private List<String> proceduresWithForever;
    private List<String> calledProcedures;
    private boolean insideProcedure;
    private Map<Identifier, ProcedureInfo> procMap;
    private Program program;

    @Override
    public IssueReport check(Program program) {
        Preconditions.checkNotNull(program);
        found = false;
        count = 0;
        actorNames = new LinkedList<>();
        this.program=program;
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
        procMap=program.getProcedureMapping().getProcedures().get(currentActor.getIdent().getName());
        calledProcedures = new ArrayList<>();
        proceduresWithForever = new ArrayList<>();
        if (!actor.getChildren().isEmpty()) {
            for (ASTNode child : actor.getChildren()) {
                child.accept(this);
            }
        }
        checkCalls();
        if (found) {
            found = false;
            actorNames.add(currentActor.getIdent().getName());
        }
    }

    private void checkCalls() {
        for (String calledProcedure : calledProcedures) {
            if (proceduresWithForever.contains(calledProcedure)) {
                found = true;
                count++;
            }
        }
    }

    @Override
    public void visit(ProcedureDefinition node) {
        insideProcedure = true;
        currentProcedureName = procMap.get(node.getIdent()).getName();

        if (!node.getChildren().isEmpty()) {
            for (ASTNode child : node.getChildren()) {
                child.accept(this);
            }
        }
        insideProcedure = false;
    }

    @Override
    public void visit(RepeatForeverStmt node) {
        if (insideProcedure) {
            proceduresWithForever.add(currentProcedureName);
        }
        if (!node.getChildren().isEmpty()) {
            for (ASTNode child : node.getChildren()) {
                child.accept(this);
            }
        }
    }

    @Override
    public void visit(StmtList node) {
        List<Stmt> stmts = node.getStmts().getListOfStmt();
        for (int i = 0; i < stmts.size() - 1; i++) {
            if (stmts.get(i) instanceof CallStmt) {
                calledProcedures.add(((CallStmt) stmts.get(i)).getIdent().getName());
            }
        }
        if (!node.getChildren().isEmpty()) {
            for (ASTNode child : node.getChildren()) {
                child.accept(this);
            }
        }
    }
}
