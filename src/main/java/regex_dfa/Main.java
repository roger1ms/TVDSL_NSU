package regex_dfa;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static List<Token> defineTokens() {
        List<Token> tokens = new ArrayList<>();

        tokens.add(new Token("FUNCTION",  Node.str("function"),  false));
        tokens.add(new Token("RETURNS",   Node.str("returns"),   false));
        tokens.add(new Token("REQUIRES",  Node.str("requires"),  false));
        tokens.add(new Token("ENSURES",   Node.str("ensures"),   false));
        tokens.add(new Token("USES",      Node.str("uses"),      false));
        tokens.add(new Token("WHILE",     Node.str("while"),     false));
        tokens.add(new Token("IF",        Node.str("if"),        false));
        tokens.add(new Token("ELSE",      Node.str("else"),      false));
        tokens.add(new Token("ASSERT",    Node.str("assert"),    false));
        tokens.add(new Token("ASSUME",    Node.str("assume"),    false));
        tokens.add(new Token("INVARIANT", Node.str("invariant"), false));
        tokens.add(new Token("FORALL",    Node.str("forall"),    false));
        tokens.add(new Token("EXISTS",    Node.str("exists"),    false));
        tokens.add(new Token("TRUE",      Node.str("true"),      false));
        tokens.add(new Token("FALSE",     Node.str("false"),     false));
        tokens.add(new Token("AND",       Node.str("and"),       false));
        tokens.add(new Token("OR",        Node.str("or"),        false));
        tokens.add(new Token("PIPE",      Node.ch('|'),             false));
        tokens.add(new Token("NOT",       Node.str("not"),       false));
        tokens.add(new Token("LENGTH",    Node.str("length"),    false));

        tokens.add(new Token("IDENT",
                Node.concat(Node.arr("a-zA-Z_"), Node.star(Node.arr("a-zA-Z0-9_"))),
                false));
        tokens.add(new Token("INT",
                Node.alt(Node.ch('0'), Node.concat(Node.arr("1-9"), Node.star(Node.arr("0-9")))),
                false));
        tokens.add(new Token("WS", Node.plus(Node.arr(" \t\r\n")), true));
        tokens.add(new Token("COMMENT",
                Node.concat(Node.str("//"), Node.star(Node.arr(" -~"))),
                true));

        tokens.add(new Token("EQ",       Node.str("=="), false));
        tokens.add(new Token("NEQ",      Node.str("!="), false));
        tokens.add(new Token("LE",       Node.str("<="), false));
        tokens.add(new Token("GE",       Node.str(">="), false));
        tokens.add(new Token("IMPL",     Node.str("->"), false));
        tokens.add(new Token("FATARROW", Node.str("=>"), false));

        tokens.add(new Token("LPAREN",   Node.ch('('), false));
        tokens.add(new Token("RPAREN",   Node.ch(')'), false));
        tokens.add(new Token("LBRACE",   Node.ch('{'), false));
        tokens.add(new Token("RBRACE",   Node.ch('}'), false));
        tokens.add(new Token("LBRACKET", Node.ch('['), false));
        tokens.add(new Token("RBRACKET", Node.ch(']'), false));
        tokens.add(new Token("COMMA",    Node.ch(','), false));
        tokens.add(new Token("SEMI",     Node.ch(';'), false));
        tokens.add(new Token("COLON",    Node.ch(':'), false));
        tokens.add(new Token("PLUS",     Node.ch('+'), false));
        tokens.add(new Token("MINUS",    Node.ch('-'), false));
        tokens.add(new Token("STAR",     Node.ch('*'), false));
        tokens.add(new Token("SLASH",    Node.ch('/'), false));
        tokens.add(new Token("LT",       Node.ch('<'), false));
        tokens.add(new Token("GT",       Node.ch('>'), false));
        tokens.add(new Token("ASSIGN",   Node.ch('='), false));

        return tokens;
    }

    public static void main(String[] args) {
        List<Token> tokens = defineTokens();

        NFA nfa = new ThompsonBuilder().union(tokens);
        DFA dfa = new SubsetConstruction().buildDFA(nfa);
        DFA minDfa = new Minimize().minimize(dfa);

        List<String> names = new ArrayList<>();
        for (Token t : tokens) {
            names.add(t.name);
        }
        minDfa.tokenNames = names;

        System.out.println("Токенов: " + tokens.size());
        System.out.println("НКА: " + nfa.allStates.size() + " состояний");
        System.out.println("ДКА до минимизации: " + dfa.size());
        System.out.println("ДКА после минимизации: " + minDfa.size());

        new TrapAdder().addTrap(minDfa);
        System.out.println("minDfa.trap_id = " + minDfa.trap);

        try {
            DFAExporter.exportCsv(minDfa, "export");
            System.out.println("Экспортировано в папку export/");
        } catch (Exception e) {
            System.out.println("Ошибка экспорта: " + e.getMessage());
        }
    }
}