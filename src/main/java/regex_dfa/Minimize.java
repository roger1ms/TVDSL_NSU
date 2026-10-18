package regex_dfa;

import java.util.*;

public class Minimize {
    public DFA minimize(DFA dfa) {
        Map<String, Set<Integer>> group = new HashMap<>();
        Set<Character> alphabet = new HashSet<>();

        for (int i : dfa.allStates) {
            alphabet.addAll(dfa.transitions.getOrDefault(i, Map.of()).keySet());

            Integer id = dfa.acceptTokenId.get(i);
            String key = (id != null) ? id.toString() : "Not accepting";
            group.computeIfAbsent(key, k -> new HashSet<>()).add(i);
        }

        Set<Set<Integer>> P = new HashSet<>();
        for (var e : group.entrySet()) {
            P.add(Set.copyOf(e.getValue()));
        }

        Queue<Map.Entry<Set<Integer>, Character>> W = new LinkedList<>();
        for (Set<Integer> set : P) {
            for (Character ch : alphabet) {
                W.offer(new AbstractMap.SimpleEntry<>(set, ch));
            }
        }

        while (!W.isEmpty()) {
            Map.Entry<Set<Integer>, Character> entry = W.poll();
            Set<Integer> set = entry.getKey();
            Character ch = entry.getValue();
            for (Set<Integer> set2 : new ArrayList<>(P)) {
                if (!P.contains(set2)) continue;
                Set<Integer> canReach = new HashSet<>();
                Set<Integer> cantReach = new HashSet<>();
                dfa.checkReachability(canReach, cantReach, set2, set, ch);
                if (canReach.isEmpty() || cantReach.isEmpty()) {
                    continue;
                } else {
                    P.remove(set2);
                    Set<Integer> canImm = Set.copyOf(canReach);
                    Set<Integer> cantImm = Set.copyOf(cantReach);
                    P.add(canImm);
                    P.add(cantImm);
                    for (char c : alphabet) {
                        W.offer(Map.entry(canImm, c));
                        W.offer(Map.entry(cantImm, c));
                    }
                }
            }
        }

        Map<Set<Integer>, Integer> classId = new HashMap<>();
        int nextId = 0;
        for (Set<Integer> cls : P) {
            classId.put(cls, nextId++);
        }

        Map<Integer, Integer> stateClass = new HashMap<>();
        for (Set<Integer> cls : P) {
            int id = classId.get(cls);
            for (int s : cls) stateClass.put(s, id);
        }

        DFA result = new DFA();
        result.tokenNames = dfa.tokenNames;

        result.start = stateClass.get(dfa.start);
        result.allStates.add(result.start);

        for (var e : dfa.acceptTokenId.entrySet()) {
            int s = e.getKey();
            int tokenId = e.getValue();
            int gid = stateClass.get(s);
            Integer existing = result.acceptTokenId.get(gid);
            if (existing == null || tokenId < existing) {
                result.acceptTokenId.put(gid, tokenId);
            }
        }

        for (int s : dfa.allStates) {
            int fromId = stateClass.get(s);
            for (var e : dfa.transitions.getOrDefault(s, Map.of()).entrySet()) {
                char c = e.getKey();
                int to = e.getValue();
                int toId = stateClass.get(to);
                result.newTransition(fromId, toId, c);
            }
        }

        return result;
    }
}