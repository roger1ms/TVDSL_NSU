package parser.AST;

import java.util.List;

public class Call extends AST {
    public final String name;
    public final List<AST> args;

    public Call(String name, List<AST> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public String toString(int indent) {
        String t = (type != null) ? " : " + type : "";
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Call(").append(name).append(")").append(t).append("\n");
        for (AST arg : args) sb.append(arg.toString(indent + 1)).append("\n");
        return sb.toString();
    }
}
