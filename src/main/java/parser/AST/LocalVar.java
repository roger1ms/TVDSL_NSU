package parser.AST;

public class LocalVar extends AST {
    public final String name;
    public final String type;

    public LocalVar(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "LocalVar(" + name
                + (type != null ? ": " + type : "") + ")";
    }
}
