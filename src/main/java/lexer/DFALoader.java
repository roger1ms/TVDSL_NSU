package lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DFALoader {
    public static DFA load(String path) throws IOException {
        DFA dfa = new DFA();

        for (String line : Files.readAllLines(Path.of(path, "tokens.csv"))) {
            String[] p = line.split(",");
            if (p[0].equals("id")) continue;
            int id = Integer.parseInt(p[0]);
            while (dfa.tokenNames.size() <= id) dfa.tokenNames.add(null);
            dfa.tokenNames.set(id, p[1]);
        }

        for (String line : Files.readAllLines(Path.of(path, "meta.csv"))) {
            String[] words = line.split(",");
            if (words[0].equals("start")) dfa.start = Integer.parseInt(words[1]);
            if (words[0].equals("trap")) dfa.trap = Integer.parseInt(words[1]);
        }

        for (String line : Files.readAllLines(Path.of(path, "accepting.csv"))) {
            String[] words = line.split(",");
            if (words[0].equals("state")) continue;
            int key = Integer.parseInt(words[0]), value = Integer.parseInt(words[1]);
            dfa.acceptTokenId.put(key, value);
        }

        for (String line : Files.readAllLines(Path.of(path, "transitions.csv"))) {
            String[] words = line.split(",");
            if (words[0].equals("state")) continue;
            int from = Integer.parseInt(words[0]), to = Integer.parseInt(words[2]);
            char c =  (char) Integer.parseInt(words[1]);
            dfa.transitions.computeIfAbsent(from, k -> new HashMap<>()).put(c, to);
        }

        dfa.allStates.addAll(dfa.transitions.keySet());
        dfa.allStates.addAll(dfa.acceptTokenId.keySet());
        dfa.allStates.add(dfa.start);
        dfa.allStates.add(dfa.trap);

        for (int s : new ArrayList<>(dfa.allStates)) {
            Map<Character, Integer> row = dfa.transitions.computeIfAbsent(s, k -> new HashMap<>());
            for (char c = 0; c < 128; c++) {
                row.putIfAbsent(c, dfa.trap);
            }
        }

        return dfa;
    }
}
