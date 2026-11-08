package lexer;

public class Main {
    public static void main(String[] args) throws Exception {
        DFA dfa = DFALoader.load("export/");

        String input1 = "f(a: int[], n: int) requires n > 0 returns r: int ensures forall (i: int | i >= 0 and i < n -> a[i] > 0) { r = 0; }";
        String input = "f() returns r: int { r = @; }";
        Lexer lexer = new Lexer(dfa, input);
        for (Token t : lexer.tokenize()) {
            System.out.println(t);
        }
    }
}