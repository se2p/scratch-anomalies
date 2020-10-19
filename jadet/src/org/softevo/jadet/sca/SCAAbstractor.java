package org.softevo.jadet.sca;


import org.softevo.jutil.graphs.Graph;
import org.softevo.oumextractor.modelcreator1.model.CastTransition;
import org.softevo.oumextractor.modelcreator1.model.EpsilonTransition;
import org.softevo.oumextractor.modelcreator1.model.ExceptionTransition;
import org.softevo.oumextractor.modelcreator1.model.FieldValueTransition;
import org.softevo.oumextractor.modelcreator1.model.InvokeMethodTransition;
import org.softevo.oumextractor.modelcreator1.model.LightweightTransition;
import org.softevo.oumextractor.modelcreator1.model.Model;
import org.softevo.oumextractor.modelcreator1.model.ReturnValueOfMethodTransition;
import org.softevo.oumextractor.modelcreator1.model.State;
import org.softevo.oumextractor.modelcreator1.model.Transition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


/**
 * This class is responsible for turning models into their sequential
 * constraints abstractions.
 *
 * @author Andrzej Wasylkowski
 */
public class SCAAbstractor {

    /**
     * Returns the sequential constraints abstraction of the given model.
     *
     * @param model  Model to analyze.
     * @param filter Indicates if filtering of constraints should be done.
     * @return Sequential constraints abstraction of the given model.
     */
    public static Set<EventPair> getSCAAbstraction(Model model,
                                                   boolean filter) {
        // get incoming and outcoming transitions for each state
        Map<State, Set<Transition>> state2incoming =
                getIncomingTransitionsMap(model, filter);
        Map<State, Set<Transition>> state2outgoing =
                getOutgoingTransitionsMap(model, filter);

        // calculate the abstraction
        Set<EventPair> sca = new HashSet<EventPair>();
        for (State s : state2incoming.keySet()) {
            for (Transition t1 : state2incoming.get(s)) {
                for (Transition t2 : state2outgoing.get(s)) {
                    EventPair pair = EventPair.get(t1, t2);
                    sca.add(pair);
                }
            }
        }

        return sca;
    }


    /**
     * Returns the mapping from states to transitions that can happen on any
     * possible path that ends in that state.
     *
     * @param model  Model to analyze.
     * @param filter Indicates if filtering of constraints should be done.
     * @return Mapping from states to incoming (transitive) transitions.
     */
    private static Map<State, Set<Transition>> getIncomingTransitionsMap(
            Model model, boolean filter) {
        // get the graph underlying the model
        Graph<State, Transition> graph = model.getUnderlyingGraph();

        // initialize the mapping with empty sets
        Map<State, Set<Transition>> result =
                new HashMap<State, Set<Transition>>();
        for (State s : graph.getVertices()) {
            result.put(s, new HashSet<Transition>());
        }

        // calculate the mapping for every state
        Queue<State> statesToConsider = new LinkedList<State>();
        for (State s : graph.getVertices()) {
            statesToConsider.offer(s);
        }
        while (!statesToConsider.isEmpty()) {
            State state = statesToConsider.poll();
            for (State next : graph.getSuccessors(state)) {
                Set<Transition> trs = graph.getEdges(state, next);
                Set<Transition> incoming = result.get(next);
                int oldIncomingSize = incoming.size();

                // add all transitively incoming transitions
                incoming.addAll(result.get(state));

                // add all directly incoming transitions
                for (Transition tr : trs) {
                    if (tr instanceof EpsilonTransition)
                        continue;
                    if (filter && filterTransition(tr))
                        continue;
                    incoming.add(tr);
                }

                // update the map if necessary
                int newIncomingSize = incoming.size();
                if (oldIncomingSize != newIncomingSize) {
                    result.put(next, incoming);
                    statesToConsider.add(next);
                }
            }
        }

        return result;
    }


    /**
     * Returns the mapping from states to transitions that can happen on any
     * possible path that starts in that state.
     *
     * @param model  Model to analyze.
     * @param filter Indicates if filtering of constraints should be done.
     * @return Mapping from states to outgoing (transitive) transitions.
     */
    private static Map<State, Set<Transition>> getOutgoingTransitionsMap(
            Model model, boolean filter) {
        // get the graph underlying the model
        Graph<State, Transition> graph = model.getUnderlyingGraph();

        // initialize the mapping with empty sets
        Map<State, Set<Transition>> result =
                new HashMap<State, Set<Transition>>();
        for (State s : graph.getVertices()) {
            result.put(s, new HashSet<Transition>());
        }

        // calculate the mapping for every state
        Queue<State> statesToConsider = new LinkedList<State>();
        for (State state : graph.getVertices()) {
            statesToConsider.offer(state);
        }
        while (!statesToConsider.isEmpty()) {
            State state = statesToConsider.poll();
            for (State prev : graph.getPredecessors(state)) {
                Set<Transition> trs = graph.getEdges(prev, state);
                Set<Transition> outgoing = result.get(prev);
                int oldOutgoingSize = outgoing.size();

                // add all transitively outgoing transitions
                outgoing.addAll(result.get(state));

                // add all directly outgoing transitions
                for (Transition tr : trs) {
                    if (tr instanceof EpsilonTransition)
                        continue;
                    if (filter && filterTransition(tr))
                        continue;
                    outgoing.add(tr);
                }

                // update the map if necessary
                int newOutgoingSize = outgoing.size();
                if (oldOutgoingSize != newOutgoingSize) {
                    result.put(prev, outgoing);
                    statesToConsider.add(prev);
                }
            }
        }

        return result;
    }


    /**
     * Checks if the given transition should be filtered (i.e., is related to
     * StringBuffer, String, StringBuilder).
     *
     * @param t Transition to check.
     * @return <code>true</code> if the transition should be filtered;
     * <code>false</code> otherwise.
     */
    private static boolean filterTransition(Transition t) {
        String[] noiseClasses = {"java.lang.String", "java.lang.StringBuffer",
                "java.lang.StringBuilder"};
        if (t instanceof CastTransition) {
            CastTransition tr = (CastTransition) t;
            for (String noiseClass : noiseClasses)
                if (tr.getType().equals(noiseClass))
                    return true;
            return false;
        } else if (t instanceof EpsilonTransition)
            return false;
        else if (t instanceof ExceptionTransition) {
            ExceptionTransition tr = (ExceptionTransition) t;
            return filterTransition(tr.getTransition());
        } else if (t instanceof FieldValueTransition) {
            FieldValueTransition tr = (FieldValueTransition) t;
            for (String noiseClass : noiseClasses)
                if (tr.getFieldType().equals(noiseClass))
                    return true;
            return false;
        } else if (t instanceof InvokeMethodTransition) {
            InvokeMethodTransition tr = (InvokeMethodTransition) t;
            for (String noiseClass : noiseClasses)
                if (tr.getMethodCall().getTypeName().equals(noiseClass))
                    return true;
            return false;
        } else if (t instanceof LightweightTransition) {
            // UNSUPPORTED: NO FILTERING DONE
            return false;
        } else if (t instanceof ReturnValueOfMethodTransition) {
            ReturnValueOfMethodTransition tr = (ReturnValueOfMethodTransition) t;
            for (String noiseClass : noiseClasses)
                if (tr.getMethodCall().getTypeName().equals(noiseClass))
                    return true;
            return false;
        } else {
            System.err.println("Unknown transition type: " + t.getClass().toString());
            throw new InternalError();
        }
    }
}
