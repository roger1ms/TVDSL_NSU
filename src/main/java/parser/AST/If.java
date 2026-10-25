package parser.AST;

public class If extends AST {
    public final AST cond;
    public final AST then;
    public final AST else_;

    public If(AST cond, AST then, AST else_) {
        this.cond = cond;
        this.then = then;
        this.else_ = else_;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("If:\n");
        sb.append(pad(indent + 1)).append("cond:\n").append(cond.toString(indent + 2)).append("\n");
        sb.append(pad(indent + 1)).append("then:\n").append(then.toString(indent + 2)).append("\n");
        if (else_ != null) {
            sb.append(pad(indent + 1)).append("else:\n").append(else_.toString(indent + 2)).append("\n");
        }
        return sb.toString();
    }
}
