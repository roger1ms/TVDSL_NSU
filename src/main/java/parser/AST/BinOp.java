package parser.AST;

public class BinOp extends AST {
    public final String op;
    public final AST left, right;

    public BinOp(String op, AST left, AST right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString(int indent) {
        String t = (type != null) ? " : " + type : "";
        return pad(indent) + "BinOp(" + op + ")" + t + "\n" +
                left.toString(indent + 1) + "\n" +
                right.toString(indent + 1);
    }
}
