package parser.AST;

public class BoolLit extends AST {
    public final boolean value;

    public BoolLit(boolean value) {
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "BoolLit(" + value + ")";
    }
}
