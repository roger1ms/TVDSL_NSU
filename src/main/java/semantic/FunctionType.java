package semantic;

import java.util.List;

public class FunctionType implements Type {
    public final List<Type> params;
    public final List<Type> returns;

    public FunctionType(List<Type> params, List<Type> returns) {
        this.params = params;
        this.returns = returns;
    }

    @Override
    public String toString() {
        return "func" + params + " -> " + returns;
    }
}