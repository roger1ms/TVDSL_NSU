package parser.AST;

public class While extends AST {
    public final AST cond;
    public final AST invariant;
    public final AST body;

    public While(AST cond, AST invariant, AST body) {
        this.cond = cond;
        this.invariant = invariant;
        this.body = body;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("While:\n");
        sb.append(pad(indent + 1)).append("cond:\n").append(cond.toString(indent + 2)).append("\n");
        if (invariant != null) {
            sb.append(pad(indent + 1)).append("invariant:\n");
            sb.append(invariant.toString(indent + 2)).append("\n");
        }
        sb.append(pad(indent + 1)).append("body:\n");
        sb.append(body.toString(indent + 2)).append("\n");
        return sb.toString();
    }
}
