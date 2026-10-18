package regex_dfa;

public class Token {
    public final String name;
    public final Node node;
    public final boolean skip;

    public Token(String name, Node node, boolean skip) {
        this.name = name;
        this.node = node;
        this.skip = skip;
    }
}
