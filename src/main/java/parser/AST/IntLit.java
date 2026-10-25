package parser.AST;

public class IntLit extends AST {
    public final int value;

    public IntLit(int value) {
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        String t = (type != null) ? " : " + type : "";
        return pad(indent) + "IntLit(" + value + ")" + t;
    }
}
