package parser.AST;

public class LocalVar extends AST {
    public final String name;
    public final String typeName;

    public LocalVar(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "LocalVar(" + name
                + (typeName != null ? ": " + typeName : "") + ")"
                + (type != null ? " : " + type : "");
    }
}
