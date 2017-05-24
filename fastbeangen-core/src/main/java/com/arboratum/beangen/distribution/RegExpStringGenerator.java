package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import org.apache.commons.math3.util.FastMath;

import java.nio.CharBuffer;
import java.util.*;
import java.util.function.Function;

/**
 * Created by gpicron on 07/08/2016.
 */
public class RegExpStringGenerator implements Function<RandomSequence, char[]> {
    private final Node rootNode;

    private static class NodeTransition {
        private final char firstChar;
        private final char lastChar;
        private final Node next;

        public NodeTransition(char firstChar, char lastChar, Node next) {
            this.firstChar = firstChar;
            this.lastChar = lastChar;
            this.next = next;
        }
        public char getFirstChar() {
            return firstChar;
        }

        public char getLastChar() {
            return lastChar;
        }

        public Node getNext() {
            return next;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeTransition that = (NodeTransition) o;
            return firstChar == that.firstChar &&
                    lastChar == that.lastChar &&
                    Objects.equals(next, that.next);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstChar, lastChar, next);
        }

        @Override
        public String toString() {
            return "NodeTransition{" +
                    "firstChar=" + firstChar +
                    ", lastChar=" + lastChar +
                    ", next=" + next +
                    '}';
        }
    }

    private static class Node {
        private long count;
        private int maxLen;
        private long[] proba;
        private NodeTransition[] transitions;

        public NodeTransition getTransition(RandomSequence register) {
            int index = Arrays.binarySearch(proba, register.nextLong(count));
            if (index < 0) {
                index = -index-1;
            }
            return transitions[index];
        }

        public void setCount(long count) {
            this.count = count;
        }

        public int getMaxLen() {
            return maxLen;
        }

        public void setMaxLen(int maxLen) {
            this.maxLen = maxLen;
        }

        public long[] getProba() {
            return proba;
        }

        public void setProba(long[] proba) {
            this.proba = proba;
        }

        public NodeTransition[] getTransitions() {
            return transitions;
        }

        public void setTransitions(NodeTransition[] transitions) {
            this.transitions = transitions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Double.compare(node.count, count) == 0 &&
                    maxLen == node.maxLen &&
                    Arrays.equals(proba, node.proba) &&
                    Arrays.equals(transitions, node.transitions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(count, maxLen, proba, transitions);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "count=" + count +
                    ", maxLen=" + maxLen +
                    ", proba=" + Arrays.toString(proba) +
                    ", transitions=" + Arrays.toString(transitions) +
                    '}';
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
        final long[] proba;
        long count = 0;
        int i = 0;
        int maxLen = 0;

        if (state.isAccept()) {
            trans = new NodeTransition[state.getTransitions().size() + 1];
            proba = new long[trans.length];
            count += 1;
            proba[0] = 1;
            i = 1;
        } else {
            trans = new NodeTransition[state.getTransitions().size()];
            proba = new long[trans.length];
        }


        Set<Transition> transitions = state.getTransitions();
        TreeSet<Transition> ordered = new TreeSet<>((a, b) -> Character.compare(a.getMin(), b.getMin()));
        ordered.addAll(transitions);
        for (Transition t : ordered) {
            long transitionCount = t.getMax() - t.getMin() + 1;

            Node childCount = nodes.get(t.getDest());
            if (childCount == null) {
                childCount = new Node();
                computeCount(childCount, t.getDest(), nodes);
            }

            count = FastMath.addExact(count, FastMath.multiplyExact(transitionCount,childCount.count));
            proba[i] = count;
            maxLen = Math.max(maxLen, 1 + childCount.getMaxLen());
            trans[i++] = new NodeTransition(t.getMin(), t.getMax(), childCount);
        }



        node.setCount(count);
        node.setTransitions(trans);
        node.setProba(proba);
        node.setMaxLen(maxLen);

        nodes.put(state, node);
    }


    public char[] apply(RandomSequence register) {
        CharBuffer value = CharBuffer.allocate(rootNode.getMaxLen());

        NodeTransition transition = rootNode.getTransition(register);
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
                transition = next.getTransition(register);
            }
        }

        value.flip();
        char[] result = new char[value.length()];
        System.arraycopy(value.array(), 0, result, 0, value.length());

        return result;
    }


    @Override
    public String toString() {
        return "RegExpStringGenerator{" +
                "rootNode=" + rootNode +
                '}';
    }

}
