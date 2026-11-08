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
}