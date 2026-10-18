package regex_dfa;
import java.util.*;

public class SubsetConstruction {
    public DFA buildDFA(NFA nfa) {
        DFA dfa = new DFA();
        Map<Set<Integer>, Integer> stateMap = new HashMap<>();
        Queue<Set<Integer>> queue = new LinkedList<>();

        Set<Integer> startSet = Set.copyOf(nfa.epsClosure(Set.of(nfa.start)));
        stateMap.put(startSet, 0);
        dfa.allStates.add(0);
        dfa.start = 0;
        queue.add(startSet);
        markAccepting(nfa, dfa, startSet, 0);

        while (!queue.isEmpty()) {
            Set<Integer> current = queue.poll();
            int curId = stateMap.get(current);

            Set<Character> symbols = new HashSet<>();
            for (int s : current) {
                symbols.addAll(nfa.transitions.getOrDefault(s, Map.of()).keySet());
            }

            for (char c : symbols) {
                Set<Integer> next = nfa.move(current, c);
                if (next.isEmpty()) continue;

                next = Set.copyOf(nfa.epsClosure(next));

                if (!stateMap.containsKey(next)) {
                    int newId = stateMap.size();
                    stateMap.put(next, newId);
                    dfa.allStates.add(newId);
                    queue.add(next);
                    markAccepting(nfa, dfa, next, newId);
                }

                int nextId = stateMap.get(next);
                dfa.newTransition(curId, nextId, c);
            }
        }
        return dfa;
    }

    private void markAccepting(NFA nfa, DFA dfa, Set<Integer> states, int id) {
        int bestId = Integer.MAX_VALUE;
        for (int s : states) {
            Integer tokenId = nfa.acceptTokenId.get(s);
            if (tokenId != null && tokenId < bestId) {
                bestId = tokenId;
            }
        }
        if (bestId != Integer.MAX_VALUE) {
            dfa.acceptTokenId.put(id, bestId);
        }
    }
}