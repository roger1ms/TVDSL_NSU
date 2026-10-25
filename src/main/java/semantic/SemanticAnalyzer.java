package semantic;

import parser.AST.*;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errors = new ArrayList<>();

    public SemanticAnalyzer() {}

    public void addError(String msg) {
        errors.add(msg);
    }

    public void addError(AST node, String msg) {
        if (node != null && node.line != -1) {
            errors.add("[" + node.line + ":" + node.col + "] " + msg);
        } else {
            errors.add(msg);
        }
    }

    public void analyze(Program program) {
        for (AST item : program.items) {
            if (item instanceof Function func) {
                List<Type> params = new ArrayList<>();
                for (Param p : func.params) {
                    params.add(resolveType(p.type));
                }

                List<Type> returns = new ArrayList<>();
                for (Param p : func.returns) {
                    returns.add(resolveType(p.type));
                }

                FunctionType funcType = new FunctionType(params, returns);
                Symbol funcSym = new Symbol(func.name, funcType);
                if (!symbolTable.define(funcSym)) {
                    addError(func, "Symbol " + funcSym.name + " already exists");
                }
            }
        }

        for (AST item : program.items) {
            if (item instanceof Function func) {
                visitFunction(func);
            }
        }
    }

    private void visitFunction(Function func) {
        symbolTable.enterScope();

        for (Param p : func.params) {
            Type type = resolveType(p.type);
            Symbol sym = new Symbol(p.name, type);
            if (!symbolTable.define(sym)) {
                addError(p, "Parameter '" + p.name + "' is already defined");
            }
        }

        for (Param p : func.returns) {
            Type type = resolveType(p.type);
            Symbol sym = new Symbol(p.name, type);
            if (!symbolTable.define(sym)) {
                addError(p, "Return variable '" + p.name + "' is already defined");
            }
        }

        for (LocalVar lv : func.uses) {
            Type type = resolveType(lv.type);
            Symbol sym = new Symbol(lv.name, type);
            if (!symbolTable.define(sym)) {
                addError(lv, "Local variable '" + lv.name + "' is already defined");
            }
        }

        if (func.requires != null) {
            Type reqType = visitExpr(func.requires);
            if (reqType != PrimitiveType.BOOL && reqType != PrimitiveType.ERROR) {
                addError(func.requires, "Requires condition must be boolean, got " + reqType);
            }
        }

        if (func.ensures != null) {
            Type ensType = visitExpr(func.ensures);
            if (ensType != PrimitiveType.BOOL && ensType != PrimitiveType.ERROR) {
                addError(func.ensures, "Ensures condition must be boolean, got " + ensType);
            }
        }

        visitStatement(func.body);
        symbolTable.exitScope();
    }

    private Type resolveType(String typeStr) {
        if (typeStr == null) return PrimitiveType.ERROR;
        return switch (typeStr) {
            case "int" -> PrimitiveType.INT;
            case "int[]" -> PrimitiveType.INT_ARRAY;
            default -> {
                addError("Unknown type " + typeStr);
                yield PrimitiveType.ERROR;
            }
        };
    }

    private void visitStatement(AST statement) {
        if (statement instanceof Block block) {
            symbolTable.enterScope();
            for (AST s : block.stmts) {
                visitStatement(s);
            }
            symbolTable.exitScope();
        }

        else if (statement instanceof Assign assign) {
            Type targetType = visitExpr(assign.target);
            Type valueType = visitExpr(assign.value);

            if (targetType != PrimitiveType.ERROR && valueType != PrimitiveType.ERROR) {
                if (targetType != valueType) {
                    addError(assign, "Type mismatch in assignment: cannot assign " +
                            valueType + " to " + targetType);
                }
            }
        }

        else if (statement instanceof If ifStmt) {
            Type condType = visitExpr(ifStmt.cond);
            if (condType != PrimitiveType.BOOL && condType != PrimitiveType.ERROR) {
                addError(ifStmt.cond, "If condition must be boolean, got " + condType);
            }

            visitStatement(ifStmt.then);
            if (ifStmt.else_ != null) {
                visitStatement(ifStmt.else_);
            }
        }

        else if (statement instanceof While whileStmt) {
            Type condType = visitExpr(whileStmt.cond);
            if (condType != PrimitiveType.BOOL && condType != PrimitiveType.ERROR) {
                addError(whileStmt.cond, "While condition must be boolean, got " + condType);
            }

            if (whileStmt.invariant != null) {
                Type invType = visitExpr(whileStmt.invariant);
                if (invType != PrimitiveType.BOOL && invType != PrimitiveType.ERROR) {
                    addError(whileStmt.invariant, "While invariant must be boolean, got " + invType);
                }
            }

            visitStatement(whileStmt.body);
        }

        else if (statement instanceof Assert assertStmt) {
            Type predType = visitExpr(assertStmt.pred);
            if (predType != PrimitiveType.BOOL && predType != PrimitiveType.ERROR) {
                addError(assertStmt.pred, "Assert condition must be boolean, got " + predType);
            }
        }

        else if (statement instanceof Assume assumeStmt) {
            Type predType = visitExpr(assumeStmt.pred);
            if (predType != PrimitiveType.BOOL && predType != PrimitiveType.ERROR) {
                addError(assumeStmt.pred, "Assume condition must be boolean, got " + predType);
            }
        }

        else {
            if (statement != null) {
                addError(statement, "Unsupported statement type: " + statement.getClass().getSimpleName());
            }
        }
    }

    private Type visitExpr(AST expr) {
        if (expr instanceof IntLit) {
            expr.type = PrimitiveType.INT;
            return PrimitiveType.INT;
        }

        else if (expr instanceof Ident ident) {
            Symbol sym = symbolTable.lookup(ident.name);
            if (sym == null) {
                addError(ident, "Undeclared variable: " + ident.name);
                return PrimitiveType.ERROR;
            }
            expr.type = sym.type;
            return sym.type;
        }

        else if (expr instanceof BinOp binOp) {
            Type left = visitExpr(binOp.left);
            Type right = visitExpr(binOp.right);

            if (binOp.op.equals("and") || binOp.op.equals("or") || binOp.op.equals("->")) {
                if (left == PrimitiveType.BOOL && right == PrimitiveType.BOOL) {
                    expr.type = PrimitiveType.BOOL;
                    return PrimitiveType.BOOL;
                }
                if (left != PrimitiveType.ERROR && right != PrimitiveType.ERROR) {
                    addError(binOp, "Logical operation '" + binOp.op + "' requires bool, got " + left + " and " + right);
                }
                return PrimitiveType.ERROR;
            }

            if (left == PrimitiveType.INT && right == PrimitiveType.INT) {
                expr.type = PrimitiveType.INT;
                return PrimitiveType.INT;
            }
            if (left != PrimitiveType.ERROR && right != PrimitiveType.ERROR) {
                addError(binOp, "Arithmetic operation '" + binOp.op + "' requires int, got " + left + " and " + right);
            }
            return PrimitiveType.ERROR;
        }

        else if (expr instanceof Compare cmp) {
            Type left = visitExpr(cmp.left);
            Type right = visitExpr(cmp.right);

            if (left == PrimitiveType.INT && right == PrimitiveType.INT) {
                expr.type = PrimitiveType.BOOL;
                return PrimitiveType.BOOL;
            }
            if (left != PrimitiveType.ERROR && right != PrimitiveType.ERROR) {
                addError(cmp, "Comparison requires int, got " + left + " and " + right);
            }
            return PrimitiveType.ERROR;
        }

        else if (expr instanceof Index index) {
            Type arrayType = visitExpr(index.array);
            Type idxType = visitExpr(index.index);

            if (arrayType != PrimitiveType.INT_ARRAY && arrayType != PrimitiveType.ERROR) {
                addError(index, "Cannot index non-array type " + arrayType);
            }
            if (idxType != PrimitiveType.INT && idxType != PrimitiveType.ERROR) {
                addError(index, "Array index must be int, got " + idxType);
            }
            if (arrayType == PrimitiveType.INT_ARRAY) {
                expr.type = PrimitiveType.INT;
                return PrimitiveType.INT;
            }
            return PrimitiveType.ERROR;
        }

        else if (expr instanceof Quantifier quant) {
            Type varType = resolveType(quant.varType);
            Symbol sym = new Symbol(quant.name, varType);

            symbolTable.enterScope();
            symbolTable.define(sym);

            Type predType = visitExpr(quant.pred);
            symbolTable.exitScope();

            if (predType != PrimitiveType.BOOL && predType != PrimitiveType.ERROR) {
                addError(quant, "Quantifier body must be boolean, got " + predType);
            }

            expr.type = PrimitiveType.BOOL;
            return PrimitiveType.BOOL;
        }

        else if (expr instanceof Call call) {
            Symbol sym = symbolTable.lookup(call.name);
            if (sym == null) {
                addError(call, "Undeclared function: " + call.name);
                return PrimitiveType.ERROR;
            }

            if (!(sym.type instanceof FunctionType funcType)) {
                addError(call, "'" + call.name + "' is not a function");
                return PrimitiveType.ERROR;
            }

            if (call.args.size() != funcType.params.size()) {
                addError(call, "Function '" + call.name + "' expects " +
                        funcType.params.size() + " arguments, but got " + call.args.size());
                return PrimitiveType.ERROR;
            }

            for (int i = 0; i < call.args.size(); i++) {
                Type argType = visitExpr(call.args.get(i));
                Type expectedType = funcType.params.get(i);

                if (argType != expectedType && argType != PrimitiveType.ERROR) {
                    addError(call.args.get(i), "Argument " + (i + 1) + " of '" + call.name +
                            "' expects " + expectedType + ", but got " + argType);
                }
            }

            if (!funcType.returns.isEmpty()) {
                expr.type = funcType.returns.getFirst();
                return funcType.returns.getFirst();
            }
            return PrimitiveType.VOID;
        }

        return PrimitiveType.ERROR;
    }

    public List<String> getErrors() {
        return errors;
    }
}