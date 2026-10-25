package semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

public class SymbolTable {
    private Deque<HashMap<String, Symbol>> scopes = new ArrayDeque<>();

    public SymbolTable() { enterScope(); }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    public boolean define(Symbol symbol) {
        HashMap<String, Symbol> scope = scopes.peek();
        if (scope == null) return false;

        if (scope.containsKey(symbol.name)) {
            return false;
        }
        scope.put(symbol.name, symbol);
        return true;
    }

    public Symbol lookup(String name) {
        for (HashMap<String, Symbol> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }
}
