package lexer;

import java.util.*;

public class DFA {
    public int start, trap;
    public Map<Integer, Map<Character, Integer>> transitions = new HashMap<>();
    public Map<Integer, Integer> acceptTokenId = new HashMap<>();
    public List<String> tokenNames = new ArrayList<>();
    public Set<Integer> allStates = new HashSet<>();
}