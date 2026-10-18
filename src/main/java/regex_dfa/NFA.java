package regex_dfa;

import java.util.*;

public class NFA {
    public int start;
    public Map<Integer, Integer> acceptTokenId = new HashMap<>();
    public Map<Integer, Map<Character, Set<Integer>>> transitions = new HashMap<>();
    public Map<Integer, Set<Integer>> epsTransitions = new HashMap<>();
    public Set<Integer> allStates = new HashSet<>();

    public int newState() {
        int newState = allStates.size();
        allStates.add(newState);
        return newState;
    }

    public void newTransition(int from, int to, char transit) {
        transitions.computeIfAbsent(from, k -> new HashMap<>()).computeIfAbsent(transit,
                k -> new HashSet<>()).add(to);
        allStates.add(to);
        allStates.add(from);
    }

    public void newEpsTransition(int from, int to) {
        epsTransitions.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        allStates.add(to);
        allStates.add(from);
    }

    public Set<Integer> epsClosure(Set<Integer> states) {
        Set<Integer> closure = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>(states);
        while (!queue.isEmpty()) {
            int state = queue.poll();
            if (closure.add(state)) {
                Set<Integer> stateClosure = epsTransitions.getOrDefault(state, Collections.emptySet());
                queue.addAll(stateClosure);
            }
        }
        return closure;
    }

    public Set<Integer> move(Set<Integer> states, char c) {
        Set<Integer> result = new HashSet<>();
        for (int s : states) {
            Map<Character, Set<Integer>> row = transitions.getOrDefault(s, Collections.emptyMap());
            result.addAll(row.getOrDefault(c, Collections.emptySet()));
        }
        return result;
    }

    public void print() {
        System.out.println("NFA");
        System.out.println("start: " + start);
        System.out.println("states: " + allStates.size());
        System.out.println("accepting (state → tokenId): " + acceptTokenId);
        System.out.println("transitions:");
        for (var e : transitions.entrySet()) {
            int from = e.getKey();
            for (var e2 : e.getValue().entrySet()) {
                for (int to : e2.getValue()) {
                    System.out.println("  " + from + " --" + e2.getKey() + "--> " + to);
                }
            }
        }
        System.out.println("ε-transitions:");
        for (var e : epsTransitions.entrySet()) {
            for (int to : e.getValue()) {
                System.out.println("  " + e.getKey() + " --ε--> " + to);
            }
        }
    }
}