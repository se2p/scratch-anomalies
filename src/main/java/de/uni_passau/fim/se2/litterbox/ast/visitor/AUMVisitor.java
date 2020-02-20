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
package de.uni_passau.fim.se2.litterbox.ast.visitor;

import de.uni_passau.fim.se2.litterbox.ast.model.ActorDefinition;
import de.uni_passau.fim.se2.litterbox.ast.model.Program;
import de.uni_passau.fim.se2.litterbox.ast.model.Script;
import de.uni_passau.fim.se2.litterbox.ast.model.event.Event;
import org.softevo.oumextractor.modelcreator1.model.InvokeMethodTransition;
import org.softevo.oumextractor.modelcreator1.model.MethodCall;
import org.softevo.oumextractor.modelcreator1.model.Model;
import org.softevo.oumextractor.modelcreator1.model.State;

import java.util.ArrayList;

/**
 * Visitor used for creating actor usage models of scratch programs.
 */
public class AUMVisitor implements ScratchVisitor {

    /**
     * The actor usage model created in this visitor.
     */
    private Model model;

    /**
     * The entry state of the model.
     */
    private State entryState;

    /**
     * Creates an actor usage model for this program.
     *
     * @param program The program of which the actor usage model is to be created.
     */
    @Override
    public void visit(Program program) {
        System.out.println(program.getIdent().getName());

        model = new Model();
        entryState = model.getEntryState();
        for (ActorDefinition defintion : program.getActorDefinitionList().getDefintions()) {
            defintion.accept(this);
        }
        System.out.println(model);
        //TODO
    }

    /**
     * Visits every script of this actor.
     *
     * @param actorDefinition The definition of the actor.
     */
    @Override
    public void visit(ActorDefinition actorDefinition) {
        for (Script script : actorDefinition.getScripts().getScriptList()) {
            script.accept(this);
        }
    }

    /**
     * Does the magic. Work in progress. TODO
     *
     * @param script A script of an actor. TODO
     */
    @Override
    public void visit(Script script) {
        Event event = script.getEvent();
        State state = model.getNewState();
        MethodCall methodCall = new MethodCall("sprite", event.getUniqueName());
        InvokeMethodTransition transition = InvokeMethodTransition.get(methodCall, new ArrayList<>());
        model.addTransition(entryState, state, transition);
        //TODO
    }
}
