package lexer;

public class Main {
    public static void main(String[] args) throws Exception {
        DFA dfa = DFALoader.load("export/");

        String input = "a b c d e f";

        Lexer lexer = new Lexer(dfa, input);
        for (Token t : lexer.tokenize()) {
            System.out.println(t);
        }
    }
}