package lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final DFA dfa;
    private final String input;
    private int pos;
    private int line;
    private int col;

    public Lexer(DFA dfa, String input) {
        this.dfa = dfa;
        this.input = input;
        this.pos = 0;
        this.line = 1;
        this.col = 1;
    }

    public List<Token> tokenize() {
        List<Token> result = new ArrayList<>();
        while (pos < input.length()) {
            Token t = nextToken();
            if (!t.skip) result.add(t);
        }

        result.add(new Token(-1, "EOF", "", line, col, false));
        return result;
    }

    private Token nextToken() {
        int startLine = line;
        int startCol = col;
        int startPos = pos;

        int state = dfa.start;
        int lastAcceptPos = -1;
        int lastAcceptId = -1;
        int i = pos;

        while (i < input.length()) {
            char c = input.charAt(i);
            if (c >= 128) break;
            state = dfa.transitions.get(state).get(c);
            if (state == dfa.trap) break;
            i++;
            Integer tokenId = dfa.acceptTokenId.get(state);
            if (tokenId != null) {
                lastAcceptPos = i;
                lastAcceptId = tokenId;
            }
        }

        if (lastAcceptId == -1) {
            char bad = (i < input.length()) ? input.charAt(i) : '?';
            System.err.printf("Unexpected symbol at %d:%d - '%c'%n", line, col, bad);
            pos++;
            col++;
            return new Token(-1, "ERROR", String.valueOf(bad), startLine, startCol, false);
        }

        String lexeme = input.substring(startPos, lastAcceptPos);

        for (int j = startPos; j < lastAcceptPos; j++) {
            if (input.charAt(j) == '\n') { line++; col = 1; }
            else col++;
        }

        pos = lastAcceptPos;
        String name = dfa.tokenNames.get(lastAcceptId);
        boolean skip = name.equals("WS") || name.equals("COMMENT");
        return new Token(lastAcceptId, name, lexeme, startLine, startCol, skip);
    }
}