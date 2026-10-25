package parser.AST;

public class Assume extends AST {
    public final AST pred;

    public Assume(AST pred) {
        this.pred = pred;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Assume:\n"
                + pred.toString(indent + 1);
    }
}
