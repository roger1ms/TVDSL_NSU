package regex_dfa;

import regex_dfa.Node.*;

import java.util.*;

public class ThompsonBuilder {
    public int copyInto(NFA src, NFA dest) {
        int offset = dest.allStates.size();
        for (int s : src.allStates) {
            dest.allStates.add(s + offset);
        }

        for (var e : src.transitions.entrySet()) {
            int from = e.getKey() + offset;
            for (var e2 : e.getValue().entrySet()) {
                char c = e2.getKey();
                for (int to : e2.getValue()) {
                    dest.newTransition(from, to + offset, c);
                }
            }
        }

        for (var e : src.epsTransitions.entrySet()) {
            int from = e.getKey() + offset;
            for (int to : e.getValue()) {
                dest.newEpsTransition(from, to + offset);
            }
        }

        return offset;
    }

    public NFA buildNFA(Node regular) {
        NFA nfa = new NFA();
        if (regular instanceof Char ch) {
            int s0 = nfa.newState(), s1 = nfa.newState();
            nfa.newTransition(s0, s1, ch.c);
            nfa.start = s0;
            nfa.acceptTokenId.put(s1, null);
        }
        else if (regular instanceof CharSet chSet) {
            int s0 = nfa.newState(), s1 = nfa.newState();
            for (char c : chSet.chars) {
                nfa.newTransition(s0, s1, c);
            }
            nfa.start = s0;
            nfa.acceptTokenId.put(s1, null);
        }
        else if (regular instanceof Empty) {
            int s0 = nfa.newState(), s1 = nfa.newState();
            nfa.newEpsTransition(s0, s1);
            nfa.start = s0;
            nfa.acceptTokenId.put(s1, null);
        }
        else if (regular instanceof Concat concat) {
            NFA left = buildNFA(concat.left);
            NFA right = buildNFA(concat.right);

            int offsetL = copyInto(left, nfa);
            int offsetR = copyInto(right, nfa);

            for (int f : left.acceptTokenId.keySet()) {
                nfa.newEpsTransition(f + offsetL, right.start + offsetR);
            }

            nfa.start = left.start + offsetL;
            nfa.acceptTokenId.clear();
            for (int f : right.acceptTokenId.keySet()) {
                nfa.acceptTokenId.put(f + offsetR, null);
            }
        }
        else if (regular instanceof Alt alt) {
            NFA left  = buildNFA(alt.left);
            NFA right = buildNFA(alt.right);

            int s0 = nfa.newState();
            int s1 = nfa.newState();

            int offsetL = copyInto(left,  nfa);
            int offsetR = copyInto(right, nfa);

            nfa.newEpsTransition(s0, left.start  + offsetL);
            nfa.newEpsTransition(s0, right.start + offsetR);

            for (int f : left.acceptTokenId.keySet()) {
                nfa.newEpsTransition(f + offsetL, s1);
            }
            for (int f : right.acceptTokenId.keySet()) {
                nfa.newEpsTransition(f + offsetR, s1);
            }

            nfa.start = s0;
            nfa.acceptTokenId.clear();
            nfa.acceptTokenId.put(s1, null);
        }
        else if (regular instanceof Star star) {
            int s0 = nfa.newState();
            int s1 = nfa.newState();

            NFA src = buildNFA(star.lang);
            int offset = copyInto(src, nfa);

            nfa.newEpsTransition(s0, s1);
            nfa.newEpsTransition(s0, src.start + offset);
            for (int i : src.acceptTokenId.keySet()) {
                nfa.newEpsTransition(i+offset, src.start + offset);
                nfa.newEpsTransition(i+offset, s1);
            }
            nfa.start = s0;
            nfa.acceptTokenId.clear();
            nfa.acceptTokenId.put(s1, null);
        }
        else if (regular instanceof Iteration iter) {
            Node expand = Node.concat(iter.lang, Node.star(iter.lang));
            return buildNFA(expand);
        }
        return nfa;
    }

    public NFA union(List<Token> tokens) {
        NFA result = new NFA();
        int start = result.newState();
        result.start = start;

        int id = 0;
        for (Token token : tokens) {
            NFA nfa = buildNFA(token.node);
            int offset = copyInto(nfa, result);
            result.newEpsTransition(start, nfa.start + offset);

            for (int f : nfa.acceptTokenId.keySet()) {
                int newState = f + offset;
                result.acceptTokenId.put(newState, id);
            }
            id++;
        }
        return result;
    }
}