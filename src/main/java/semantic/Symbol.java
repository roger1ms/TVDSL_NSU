package semantic;

public class Symbol {
    public String name;
    public Type type;

    public Symbol(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return name + " : " + type;
    }
}
