package regex_dfa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DFAExporter {

    public static void exportCsv(DFA dfa, String dir) throws IOException {
        Path base = Path.of(dir);
        Files.createDirectories(base);

        // 1. transitions.csv
        StringBuilder tr = new StringBuilder();
        tr.append("state,charCode,next\n");
        for (int s = 0; s < dfa.size(); s++) {
            Map<Character, Integer> row = dfa.transitions.getOrDefault(s, Map.of());
            for (var e : row.entrySet()) {
                tr.append(s).append(",")
                        .append((int) e.getKey()).append(",")
                        .append(e.getValue()).append("\n");
            }
        }
        Files.writeString(base.resolve("transitions.csv"), tr.toString());

        // 2. accepting.csv
        StringBuilder ac = new StringBuilder();
        ac.append("state,tokenId\n");
        for (var e : dfa.acceptTokenId.entrySet()) {
            ac.append(e.getKey()).append(",").append(e.getValue()).append("\n");
        }
        Files.writeString(base.resolve("accepting.csv"), ac.toString());

        // 3. tokens.csv
        StringBuilder tk = new StringBuilder();
        tk.append("id,name\n");
        for (int i = 0; i < dfa.tokenNames.size(); i++) {
            tk.append(i).append(",").append(dfa.tokenNames.get(i)).append("\n");
        }
        Files.writeString(base.resolve("tokens.csv"), tk.toString());

        // 4. meta.csv (start, trap)
        StringBuilder meta = new StringBuilder();
        meta.append("key,value\n");
        meta.append("start,").append(dfa.start).append("\n");
        meta.append("trap,").append(dfa.trap).append("\n");
        Files.writeString(base.resolve("meta.csv"), meta.toString());
    }
}