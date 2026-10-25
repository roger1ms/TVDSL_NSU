package parser.AST;

public class Index extends AST {
    public final AST array;
    public final AST index;

    public Index(AST array, AST index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public String toString(int indent) {
        String t = (type != null) ? " : " + type : "";
        return pad(indent) + "Index" + t + "\n"
                + pad(indent + 1) + "array:\n" + array.toString(indent + 2) + "\n"
                + pad(indent + 1) + "index:\n" + index.toString(indent + 2);
    }
}
