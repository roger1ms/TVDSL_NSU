package parser.AST;
import semantic.Type;

public abstract class AST {
    public Type type;
    public int line = -1;
    public int col = -1;
    public abstract String toString(int indent);

    protected String pad(int n) {
        return "  ".repeat(n);
    }
}
