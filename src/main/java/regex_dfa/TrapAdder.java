package regex_dfa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrapAdder {
    public void addTrap(DFA dfa) {
        Set<Character> alphabet = new HashSet<Character>();
        for (char c = 0; c < 128; c++) alphabet.add(c);

        int trap = dfa.newState();
        dfa.trap = trap;
        for (int state : new ArrayList<>(dfa.allStates)) {
            if (state == trap) continue;
            for (char c : alphabet) {
                if (!dfa.transitions.getOrDefault(state, Map.of()).containsKey(c)) {
                    dfa.newTransition(state, trap, c);
                }
            }
        }

        for (char c : alphabet) {
            dfa.newTransition(trap, trap, c);
        }
    }
}
