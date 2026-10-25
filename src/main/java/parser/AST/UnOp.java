package parser.AST;

public class UnOp extends AST {
    public final String op;
    public final AST operand;

    public UnOp(String op, AST operand) {
        this.op = op;
        this.operand = operand;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("UnOp(").append(op).append(")\n");
        sb.append(operand.toString(indent + 1)).append("\n");
        return sb.toString();
    }
}
