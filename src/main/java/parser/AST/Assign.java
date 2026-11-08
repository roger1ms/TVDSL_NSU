package parser.AST;

public class Assign extends AST {
    public final AST target;
    public final AST value;

    public Assign(AST target, AST value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Assign:\n"
                + pad(indent + 1) + "target:\n" + target.toString(indent + 2) + "\n"
                + pad(indent + 1) + "value:\n" + value.toString(indent + 2) + "\n";
    }
}
