package parser.AST;

public class Assert extends AST {
    public final AST pred;

    public Assert(AST pred) {
        this.pred = pred;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Assert:\n"
                + pred.toString(indent + 1);
    }
}
