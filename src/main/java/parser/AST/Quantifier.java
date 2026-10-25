package parser.AST;

public class Quantifier extends AST {
    public final boolean isForall;
    public final String name;
    public final String varType;
    public final AST pred;

    public Quantifier(boolean isForall, String name, String varType, AST pred) {
        this.isForall = isForall;
        this.name = name;
        this.varType = varType;
        this.pred = pred;
    }

    @Override
    public String toString(int indent) {
        String t = (type != null) ? " : " + type : "";
        String sb = isForall ? "Forall" : "Exists";
        return pad(indent) + sb + "(" + name + ": " + varType + ")" + t + "\n" +
                pred.toString(indent + 1);
    }
}
