package parser.AST;

import java.util.List;

public class Function extends AST {
    public final String name;
    public final List<Param> params;
    public final List<Param> returns;
    public final AST requires;
    public final AST ensures;
    public final List<LocalVar> uses;
    public final AST body;

    public Function(String name, List<Param> params,  List<Param> returns,
                    AST requires, AST ensures, List<LocalVar> uses, AST body) {
        this.name = name;
        this.params = params;
        this.returns = returns;
        this.requires = requires;
        this.ensures = ensures;
        this.uses = uses;
        this.body = body;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Function(").append(name).append(")\n");
        sb.append(pad(indent+1)).append("params:\n");
        for (Param p : params) sb.append(p.toString(indent + 2)).append("\n");

        if (!uses.isEmpty()) {
            sb.append(pad(indent+1)).append("uses:\n");
            for (LocalVar u : uses) sb.append(u.toString(indent+2)).append("\n");
        }

        sb.append(pad(indent + 1)).append("returns:\n");
        for (Param p : returns) sb.append(p.toString(indent + 2)).append("\n");

        if (requires != null) {
            sb.append(pad(indent + 1)).append("requires:\n");
            sb.append(requires.toString(indent + 2)).append("\n");
        }

        if (ensures != null) {
            sb.append(pad(indent + 1)).append("ensures:\n");
            sb.append(ensures.toString(indent + 2)).append("\n");
        }

        sb.append(pad(indent + 1)).append("body:\n");
        sb.append(body.toString(indent + 2));
        return sb.toString();
    }
}

