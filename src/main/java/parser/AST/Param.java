package parser.AST;

public class Param extends AST {
    public final String name;
    public final String typeName;

    public Param(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Param(" + name + ":" + typeName + ")"
                + (type != null ? " : " + type : "");
    }
}
