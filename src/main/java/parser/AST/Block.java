package parser.AST;

import java.util.List;

public class Block extends AST {
    public final List<AST> stmts;

    public Block(List<AST> stmts) {
        this.stmts = stmts;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Block:\n");
        for (AST stmt : stmts) sb.append(stmt.toString(indent + 1));
        return sb.toString();
    }
}
