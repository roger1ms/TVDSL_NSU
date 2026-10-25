package parser.AST;

import java.util.List;

public class Program extends AST {
    public final List<AST> items;
    public Program(List<AST> items) { this.items = items; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Program:\n");
        for (AST item : items) {
            sb.append(item.toString(indent + 1)).append("\n");
        }
        return sb.toString();
    }
}
