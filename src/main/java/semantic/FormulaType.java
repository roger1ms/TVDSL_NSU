package semantic;

import java.util.List;

public class FormulaType implements Type {
    public final List<Type> params;

    public FormulaType(List<Type> params) {
        this.params = params;
    }
}
