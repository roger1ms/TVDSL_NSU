package parser.AST;

public class Param extends AST {
    public final String name;
    public final String type;

    public Param(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Param(" + name + ":" + type + ")";
    }
}

