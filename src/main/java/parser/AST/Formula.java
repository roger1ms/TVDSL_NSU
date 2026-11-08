package parser.AST;

import java.util.List;

public class Formula extends AST {
    public final String name;
    public final List<Param> params;
    public final AST predicate;

    public Formula(String name, List<Param> params, AST predicate) {
        this.name = name;
        this.params = params;
        this.predicate = predicate;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(pad(indent)).append("Formula(").append(name).append(")\n");
        for (Param p : params) sb.append(p.toString(indent + 1)).append("\n");
        return sb.append(predicate.toString(indent + 1)).toString();
    }
}
