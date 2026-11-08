package semantic;

import parser.AST.*;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

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
                    Type type = resolveType(p.typeName, p);
                    p.type = type;
                    params.add(type);
                }

                List<Type> returns = new ArrayList<>();
                for (Param p : func.returns) {
                    Type type = resolveType(p.typeName, p);
                    p.type = type;
                    returns.add(type);
                }

                FunctionType funcType = new FunctionType(params, returns);
                Symbol funcSym = new Symbol(func.name, funcType);
                if (!symbolTable.define(funcSym)) {
                    addError(func, "Symbol " + funcSym.name + " already exists");
                }
            } else if (item instanceof Formula formula) {
                List<Type> params = new ArrayList<>();
                for (Param p : formula.params) {
                    Type type = resolveType(p.typeName, p);
                    p.type = type;
                    params.add(type);
                }
                if (!symbolTable.define(new Symbol(formula.name, new FormulaType(params)))) {
                    addError(formula, "Symbol " + formula.name + " already exists");
                }
            }
        }

        for (AST item : program.items) {
            if (item instanceof Function func) {
                visitFunction(func);
            } else if (item instanceof Formula formula) {
                visitFormula(formula);
            }
        }
    }

    private void visitFormula(Formula formula) {
        symbolTable.enterScope();
        for (Param p : formula.params) {
            if (!symbolTable.define(new Symbol(p.name, p.type))) {
                addError(p, "Parameter '" + p.name + "' is already defined");
            }
        }
        Type result = visitExpr(formula.predicate);
        if (result != PrimitiveType.BOOL && result != PrimitiveType.ERROR) {
            addError(formula.predicate, "Formula predicate must be boolean, got " + result);
        }
        symbolTable.exitScope();
    }

    private void visitFunction(Function func) {
        symbolTable.enterScope();
        Map<LocalVar, Symbol> locals = new LinkedHashMap<>();

        for (Param p : func.params) {
            Type type = p.type;
            Symbol sym = new Symbol(p.name, type);
            if (!symbolTable.define(sym)) {
                addError(p, "Parameter '" + p.name + "' is already defined");
            }
        }

        for (Param p : func.returns) {
            Type type = p.type;
            Symbol sym = new Symbol(p.name, type);
            if (!symbolTable.define(sym)) {
                addError(p, "Return variable '" + p.name + "' is already defined");
            }
        }

        for (LocalVar lv : func.uses) {
            Type type = lv.typeName == null ? null : resolveType(lv.typeName, lv);
            lv.type = type;
            Symbol sym = new Symbol(lv.name, type);
            if (!symbolTable.define(sym)) {
                addError(lv, "Local variable '" + lv.name + "' is already defined");
            } else {
                locals.put(lv, sym);
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
        for (var entry : locals.entrySet()) {
            LocalVar lv = entry.getKey();
            Symbol sym = entry.getValue();
            if (sym.type == null) {
                addError(lv, "Cannot infer type of local variable '" + lv.name + "'");
                lv.type = PrimitiveType.ERROR;
            } else {
                lv.type = sym.type;
            }
        }
        symbolTable.exitScope();
    }

    private Type resolveType(String typeStr, AST node) {
        if (typeStr == null) return PrimitiveType.ERROR;
        return switch (typeStr) {
            case "int" -> PrimitiveType.INT;
            case "int[]" -> PrimitiveType.INT_ARRAY;
            default -> {
                addError(node, "Unknown type " + typeStr);
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

        else if (statement instanceof TupleAssign tuple) {
            FunctionType signature = checkFunctionCall(tuple.call);
            if (signature != null && tuple.targets.size() != signature.returns.size()) {
                addError(tuple, "Assignment expects " + signature.returns.size()
                        + " targets, got " + tuple.targets.size());
            }
            for (int i = 0; i < tuple.targets.size(); i++) {
                Ident target = tuple.targets.get(i);
                Type valueType = signature != null && i < signature.returns.size()
                        ? signature.returns.get(i) : PrimitiveType.ERROR;
                Symbol sym = symbolTable.lookup(target.name);
                if (sym != null && sym.type == null && valueType != PrimitiveType.ERROR) sym.type = valueType;
                Type targetType;
                if (sym != null && sym.type == null) {
                    target.type = PrimitiveType.ERROR;
                    targetType = PrimitiveType.ERROR;
                } else targetType = visitExpr(target);
                if (targetType != PrimitiveType.ERROR && valueType != PrimitiveType.ERROR && targetType != valueType) {
                    addError(target, "Type mismatch in assignment: cannot assign " + valueType + " to " + targetType);
                }
            }
        }

        else if (statement instanceof Assign assign) {
            Type valueType = visitExpr(assign.value);
            Type targetType;
            if (assign.target instanceof Ident ident) {
                Symbol sym = symbolTable.lookup(ident.name);
                if (sym != null && sym.type == null && valueType != PrimitiveType.ERROR) {
                    sym.type = valueType;
                }
                if (sym != null && sym.type == null) {
                    assign.target.type = PrimitiveType.ERROR;
                    targetType = PrimitiveType.ERROR;
                } else targetType = visitExpr(assign.target);
            } else {
                targetType = visitExpr(assign.target);
            }

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
        // All visited expressions, including unsuccessful ones, receive an explicit type.
        expr.type = PrimitiveType.ERROR;
        if (expr instanceof IntLit) {
            expr.type = PrimitiveType.INT;
            return PrimitiveType.INT;
        }

        else if (expr instanceof BoolLit) {
            expr.type = PrimitiveType.BOOL;
            return expr.type;
        }

        else if (expr instanceof UnOp unary) {
            Type operand = visitExpr(unary.operand);
            Type expected = unary.op.equals("not") ? PrimitiveType.BOOL : PrimitiveType.INT;
            if (operand == expected) {
                expr.type = expected;
                return expected;
            }
            if (operand != PrimitiveType.ERROR) {
                addError(unary, "Unary '" + unary.op + "' requires " + expected + ", got " + operand);
            }
            return PrimitiveType.ERROR;
        }

        else if (expr instanceof Ident ident) {
            Symbol sym = symbolTable.lookup(ident.name);
            if (sym == null) {
                addError(ident, "Undeclared variable: " + ident.name);
                return PrimitiveType.ERROR;
            }
            if (sym.type == null) {
                addError(ident, "Type of variable '" + ident.name + "' has not been inferred");
                return PrimitiveType.ERROR;
            }
            if (sym.type instanceof FunctionType || sym.type instanceof FormulaType) {
                addError(ident, "Function '" + ident.name + "' cannot be used as a variable");
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
            if (arrayType == PrimitiveType.INT_ARRAY && idxType == PrimitiveType.INT) {
                expr.type = PrimitiveType.INT;
                return PrimitiveType.INT;
            }
            return PrimitiveType.ERROR;
        }

        else if (expr instanceof Quantifier quant) {
            Type varType = resolveType(quant.varType, quant);
            Symbol sym = new Symbol(quant.name, varType);

            symbolTable.enterScope();
            symbolTable.define(sym);

            Type predType = visitExpr(quant.pred);
            symbolTable.exitScope();

            if (predType != PrimitiveType.BOOL && predType != PrimitiveType.ERROR) {
                addError(quant, "Quantifier body must be boolean, got " + predType);
            }

            if (predType == PrimitiveType.BOOL && varType != PrimitiveType.ERROR) {
                expr.type = PrimitiveType.BOOL;
            }
            return expr.type;
        }

        else if (expr instanceof FormulaRef ref) {
            List<Type> arguments = new ArrayList<>();
            for (AST arg : ref.args) arguments.add(visitExpr(arg));
            Symbol sym = symbolTable.lookup(ref.name);
            if (sym == null) {
                addError(ref, "Undeclared formula: " + ref.name);
                return PrimitiveType.ERROR;
            }
            if (!(sym.type instanceof FormulaType formulaType)) {
                addError(ref, "'" + ref.name + "' is not a formula");
                return PrimitiveType.ERROR;
            }
            boolean valid = arguments.size() == formulaType.params.size();
            if (!valid) addError(ref, "Formula '" + ref.name + "' expects " + formulaType.params.size()
                    + " arguments, but got " + arguments.size());
            for (int i = 0; i < Math.min(arguments.size(), formulaType.params.size()); i++) {
                Type actual = arguments.get(i), expected = formulaType.params.get(i);
                if (actual != expected || actual == PrimitiveType.ERROR) {
                    valid = false;
                    if (actual != PrimitiveType.ERROR && expected != PrimitiveType.ERROR) {
                        addError(ref.args.get(i), "Argument " + (i + 1) + " of formula '" + ref.name
                                + "' expects " + expected + ", got " + actual);
                    }
                }
            }
            if (valid) expr.type = PrimitiveType.BOOL;
            return expr.type;
        }

        else if (expr instanceof Call call) {
            if (call.name.equals("length")) {
                List<Type> argumentTypes = new ArrayList<>();
                for (AST arg : call.args) argumentTypes.add(visitExpr(arg));
                if (argumentTypes.size() != 1) {
                    addError(call, "Function 'length' expects 1 arguments, but got " + argumentTypes.size());
                } else if (argumentTypes.getFirst() == PrimitiveType.INT_ARRAY) {
                    expr.type = PrimitiveType.INT;
                } else if (argumentTypes.getFirst() != PrimitiveType.ERROR) {
                    addError(call.args.getFirst(), "Argument 1 of 'length' expects int[]");
                }
                return expr.type;
            }
            FunctionType funcType = checkFunctionCall(call);
            if (funcType == null) return PrimitiveType.ERROR;
            if (funcType.returns.size() != 1) {
                addError(call, "Function '" + call.name + "' must have exactly one return value in an expression");
                expr.type = PrimitiveType.ERROR;
                return PrimitiveType.ERROR;
            }
            expr.type = funcType.returns.getFirst();
            return expr.type;
        }

        return PrimitiveType.ERROR;
    }

    private FunctionType checkFunctionCall(Call call) {
        call.type = PrimitiveType.ERROR;
        List<Type> argumentTypes = new ArrayList<>();
        for (AST arg : call.args) argumentTypes.add(visitExpr(arg));
        Symbol sym = symbolTable.lookup(call.name);
        if (sym == null) {
            addError(call, "Undeclared function: " + call.name);
            return null;
        }
        if (!(sym.type instanceof FunctionType signature)) {
            addError(call, "'" + call.name + "' is not a function");
            return null;
        }
        boolean valid = call.args.size() == signature.params.size();
        if (!valid) addError(call, "Function '" + call.name + "' expects " + signature.params.size()
                + " arguments, but got " + call.args.size());
        for (int i = 0; i < Math.min(argumentTypes.size(), signature.params.size()); i++) {
            Type actual = argumentTypes.get(i), expected = signature.params.get(i);
            if (actual == PrimitiveType.ERROR || expected == PrimitiveType.ERROR) valid = false;
            else if (actual != expected) {
                valid = false;
                addError(call.args.get(i), "Argument " + (i + 1) + " of '" + call.name
                        + "' expects " + expected + ", but got " + actual);
            }
        }
        if (!valid) return null;
        call.type = signature;
        return signature;
    }

    public List<String> getErrors() {
        return errors;
    }
}
