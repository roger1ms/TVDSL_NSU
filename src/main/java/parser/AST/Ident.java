package parser.AST;

public class Ident extends AST {
    public final String name;

    public Ident(String name) {
        this.name = name;
    }

    @Override
    public String toString(int indent) {
        String t = (type != null) ? " : " + type : "";
        return pad(indent) + "Ident(" + name + ")" + t;
    }
}
