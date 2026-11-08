package parser.AST;

import java.util.List;

public class FormulaRef extends AST {
    public final String name;
    public final List<AST> args;

    public FormulaRef(String name, List<AST> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("FormulaRef(").append(name).append(")")
                .append(type != null ? " : " + type : "").append("\n");
        for (AST a : args) sb.append(a.toString(indent + 1)).append("\n");
        return sb.toString();
    }
}
