package lexer;

public class Main {
    public static void main(String[] args) throws Exception {
        DFA dfa = DFALoader.load("export/");

        String input = "function f(x) requires x > 0 returns r ensures r >= x uses y: int { r = x; }";

        Lexer lexer = new Lexer(dfa, input);
        for (Token t : lexer.tokenize()) {
            System.out.println(t);
        }
    }
}