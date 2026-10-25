package semantic;

public enum PrimitiveType implements Type {
    INT,
    INT_ARRAY,
    BOOL,
    VOID,
    ERROR;

    @Override
    public String toString() {
        return switch (this) {
            case INT -> "int";
            case INT_ARRAY -> "int[]";
            case BOOL -> "bool";
            case VOID -> "void";
            case ERROR -> "error";
        };
    }
}