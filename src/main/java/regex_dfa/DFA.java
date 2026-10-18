package regex_dfa;
import java.util.*;

public class DFA {
    public int start;
    public int trap;
    public Map<Integer, Map<Character, Integer>> transitions = new HashMap<>();
    public Map<Integer, Integer> acceptTokenId = new HashMap<>();
    public List<String> tokenNames = new ArrayList<>();
    public Set<Integer> allStates = new HashSet<>();

    public int size() { return allStates.size(); }

    public int newState() {
        int newState = allStates.size();
        allStates.add(newState);
        return newState;
    }

    public void newTransition(int from, int to, char transit) {
        transitions.computeIfAbsent(from, k -> new HashMap<>()).put(transit, to);
        allStates.add(to);
        allStates.add(from);
    }

    public void checkReachability(Set<Integer> canReach, Set<Integer> cantReach, Set<Integer> from,
                                  Set<Integer> to, char transit) {
        for (Integer fromState : from) {
            Integer toState = transitions.getOrDefault(fromState, Map.of()).get(transit);
            if (toState != null && to.contains(toState)) canReach.add(fromState);
            else cantReach.add(fromState);
        }
    }

    public String run(String input) {
        int state = start;
        for (char c : input.toCharArray()) {
            if (state == trap) return "ERROR";
            Integer next = transitions.getOrDefault(state, Map.of()).get(c);
            if (next == null) return "ERROR";
            state = next;
        }
        Integer tokenId = acceptTokenId.get(state);
        return (tokenId == null) ? "ERROR" : tokenNames.get(tokenId);
    }

    public void print() {
        System.out.println("DFA");
        System.out.println("start: " + start);
        System.out.println("trap: " + trap);
        System.out.println("states: " + size());
        System.out.println("accepting (state → tokenId): " + acceptTokenId);
        System.out.println("tokenNames: " + tokenNames);
        System.out.println("transitions (only non-trap):");
        for (int s = 0; s < size(); s++) {
            Map<Character, Integer> row = transitions.getOrDefault(s, Map.of());
            StringBuilder sb = new StringBuilder();
            for (var e : row.entrySet()) {
                if (e.getValue() == trap) continue;
                sb.append("'").append(escape(e.getKey())).append("'→").append(e.getValue()).append(" ");
            }
            if (!sb.isEmpty()) {
                System.out.println("  " + s + ": " + sb.toString().trim());
            }
        }
    }

    private static String escape(char c) {
        if (c == '\t') return "\\t";
        if (c == '\n') return "\\n";
        if (c == '\r') return "\\r";
        if (c == ' ')  return "␣";
        return String.valueOf(c);
    }
}