package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import lombok.Data;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

/**
 * Created by gpicron on 07/08/2016.
 */
public class RegExpStringGenerator implements Function<RandomSequence, char[]> {
    private final Node rootNode;

    @Data
    private static class NodeTransition {
        private final char firstChar;
        private final char lastChar;
        private final Node next;
    }

    @Data
    private static class Node {
        private double count;
        private int maxLen;
        private double[] proba;
        private NodeTransition[] transitions;

        public NodeTransition getTransition(double register) {
            int index = Arrays.binarySearch(proba, register);
            if (index < 0) {
                index = -index-1;
            }
            return transitions[index];
        }
    }



    public RegExpStringGenerator(final String regexp) {
        RegExp re = new RegExp(regexp);
        Automaton automaton = re.toAutomaton(true);
        automaton.determinize();
        automaton.reduce();

        if (!automaton.isFinite()) {
            automaton = automaton.intersection(Automaton.makeAnyChar().repeat(0, 20));
            automaton.determinize();
            automaton.reduce();
        }


        rootNode = new Node();
        final HashMap<State, Node> nodes = new HashMap<>();

        computeCount(rootNode, automaton.getInitialState(), nodes);



    }

    private void computeCount(Node node, State state, HashMap<State, Node> nodes) {
        final NodeTransition[] trans;
        final double[] proba;
        double count = 0;
        int i = 0;
        int maxLen = 0;

        if (state.isAccept()) {
            trans = new NodeTransition[state.getTransitions().size() + 1];
            proba = new double[trans.length];
            count += 1;
            proba[0] = 1d;
            i = 1;
        } else {
            trans = new NodeTransition[state.getTransitions().size()];
            proba = new double[trans.length];
        }


        for (Transition t : state.getTransitions()) {
            long transitionCount = t.getMax() - t.getMin() + 1;

            Node childCount = nodes.get(t.getDest());
            if (childCount == null) {
                childCount = new Node();
                computeCount(childCount, t.getDest(), nodes);
            }

            count += transitionCount * childCount.count;
            proba[i] = count;
            maxLen = Math.max(maxLen, 1 + childCount.getMaxLen());
            trans[i++] = new NodeTransition(t.getMin(), t.getMax(), childCount);
        }



        for (i = 0; i < proba.length; i++) {
            proba[i] /= count;
        }


        node.setCount(count);
        node.setTransitions(trans);
        node.setProba(proba);
        node.setMaxLen(maxLen);

        nodes.put(state, node);
    }


    public char[] apply(RandomSequence register) {
        CharBuffer value = CharBuffer.allocate(rootNode.getMaxLen());

        NodeTransition transition = rootNode.getTransition(register.nextDouble());
        while (transition != null) {
            final int charRange = transition.getLastChar() - transition.getFirstChar() + 1;
            final char c;
            if (charRange == 1) {
                c = transition.getFirstChar();
            } else {
                int charindex= register.nextInt(charRange);
                c = (char) (transition.getFirstChar() + charindex);
            }

            value.append(c);

            final Node next = transition.getNext();
            if (next.transitions.length == 1) {
                transition = next.transitions[0];
            } else {
                transition = next.getTransition(register.nextDouble());
            }
        }

        value.flip();
        char[] result = new char[value.length()];
        System.arraycopy(value.array(), 0, result, 0, value.length());

        return result;
    }
}
