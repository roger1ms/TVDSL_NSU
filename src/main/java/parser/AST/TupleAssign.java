package parser.AST;

import java.util.List;

public class TupleAssign extends AST {
    public final List<Ident> targets;
    public final Call call;

    public TupleAssign(List<Ident> targets, Call call) {
        this.targets = targets;
        this.call = call;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(pad(indent)).append("TupleAssign\n");
        for (Ident target : targets) sb.append(target.toString(indent + 1)).append("\n");
        return sb.append(call.toString(indent + 1)).toString();
    }
}
