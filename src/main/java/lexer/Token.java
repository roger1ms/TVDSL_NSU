package lexer;

public class Token {
    public final int type;
    public final String name;
    public final String lexeme;
    public final int line;
    public final int col;
    public final boolean skip;

    public Token(int type, String name, String lexeme, int line, int col, boolean skip) {
        this.type = type;
        this.name = name;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
        this.skip = skip;
    }

    @Override
    public String toString() {
        return name + "(\"" + lexeme + "\")@" + line + ":" + col;
    }
}