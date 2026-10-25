package parser.AST;

public class Compare extends AST {
    public final String op;    // "==", "!=", "<", ">", "<=", ">="
    public final AST left, right;

    public Compare(String op, AST left, AST right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString(int indent) {
        String t = (type != null) ? " : " + type : "";
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Compare(").append(op).append(")").append(t).append("\n");
        sb.append(left.toString(indent + 1)).append("\n");
        sb.append(right.toString(indent + 1));
        return sb.toString();
    }
}
