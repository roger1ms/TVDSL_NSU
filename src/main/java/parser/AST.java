package parser;

import java.util.List;

public abstract class AST {
    public abstract String toString(int indent);

    protected String pad(int n) {
        return "  ".repeat(n);
    }
}

class Program extends AST {
    public final List<AST> items;
    public Program(List<AST> items) { this.items = items; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Program:\n");
        for (AST item : items) {
            sb.append(item.toString(indent + 1)).append("\n");
        }
        return sb.toString();
    }
}

class Function extends AST {
    public final String name;
    public final List<Param> params;
    public final List<Param> returns;
    public final AST requires;
    public final AST ensures;
    public final List<LocalVar> uses;
    public final AST body;

    public Function(String name, List<Param> params,  List<Param> returns,
                    AST requires, AST ensures, List<LocalVar> uses, AST body) {
        this.name = name;
        this.params = params;
        this.returns = returns;
        this.requires = requires;
        this.ensures = ensures;
        this.uses = uses;
        this.body = body;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Function(").append(name).append(")\n");
        sb.append(pad(indent+1)).append("params:\n");
        for (Param p : params) sb.append(p.toString(indent + 2)).append("\n");

        if (!uses.isEmpty()) {
            sb.append(pad(indent+1)).append("uses:\n");
            for (LocalVar u : uses) sb.append(u.toString(indent+2)).append("\n");
        }

        sb.append(pad(indent + 1)).append("returns:\n");
        for (Param p : returns) sb.append(p.toString(indent + 2)).append("\n");

        if (requires != null) {
            sb.append(pad(indent + 1)).append("requires:\n");
            sb.append(requires.toString(indent + 2)).append("\n");
        }

        if (ensures != null) {
            sb.append(pad(indent + 1)).append("ensures:\n");
            sb.append(ensures.toString(indent + 2)).append("\n");
        }

        sb.append(pad(indent + 1)).append("body:\n");
        sb.append(body.toString(indent + 2)).append("\n");
        return sb.toString();
    }
}

class LocalVar extends AST {
    public final String name;
    public final String type;

    public LocalVar(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "LocalVar(" + name
                + (type != null ? ": " + type : "") + ")";
    }
}

class Param extends AST {
    public final String name;
    public final String type;

    public Param(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Param(" + name + ":" + type + ")";
    }
}

class Block extends AST {
    public final List<AST> stmts;
    public Block(List<AST> stmts) { this.stmts = stmts; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Block:\n");
        for (AST stmt : stmts) sb.append(stmt.toString(indent+1)).append("\n");
        return sb.toString();
    }
}

class Assign extends AST {
    public final AST target;
    public final AST value;

    public Assign(AST target, AST value) { this.target = target; this.value = value; }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Assign:\n"
                + pad(indent+1) + "target:\n" + target.toString(indent+2) + "\n"
                + pad(indent+1) + "value:\n"  + value.toString(indent+2);
    }
}

class If extends AST {
    public final AST cond;
    public final AST then;
    public final AST else_;

    public If(AST cond, AST then, AST else_) { this.cond = cond; this.then = then; this.else_ = else_; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("If:\n");
        sb.append(pad(indent+1)).append("cond:\n").append(cond.toString(indent+2)).append("\n");
        sb.append(pad(indent+1)).append("then:\n").append(then.toString(indent+2)).append("\n");
        if (else_ != null) {
            sb.append(pad(indent+1)).append("else:\n").append(else_.toString(indent+2)).append("\n");
        }
        return sb.toString();
    }
}

class While extends AST {
    public final AST cond;
    public final AST invariant;
    public final AST body;

    public While(AST cond, AST invariant, AST body) {  this.cond = cond; this.invariant = invariant; this.body = body; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("While:\n");
        sb.append(pad(indent+1)).append("cond:\n").append(cond.toString(indent+2)).append("\n");
        if (invariant != null) {
            sb.append(pad(indent+1)).append("invariant:\n");
            sb.append(invariant.toString(indent+2)).append("\n");
        }
        sb.append(pad(indent+1)).append("body:\n");
        sb.append(body.toString(indent+2)).append("\n");
        return sb.toString();
    }
}

class Assert extends AST {
    public final AST pred;
    public Assert(AST pred) { this.pred = pred; }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Assert:\n"
                + pred.toString(indent + 1);
    }
}

class Assume extends AST {
    public final AST pred;
    public Assume(AST pred) { this.pred = pred; }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Assume:\n"
                + pred.toString(indent + 1);
    }
}
class BinOp extends AST {
    public final String op;
    public final AST left, right;

    public BinOp(String op, AST left, AST right) {  this.op = op; this.left = left; this.right = right; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("BinOp(").append(op).append(")\n");
        sb.append(left.toString(indent+1)).append("\n");
        sb.append(right.toString(indent+1)).append("\n");
        return sb.toString();
    }
}

class UnOp extends AST {
    public final String op;
    public final AST operand;

    public UnOp(String op, AST operand) {  this.op = op; this.operand = operand; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("UnOp(").append(op).append(")\n");
        sb.append(operand.toString(indent+1)).append("\n");
        return sb.toString();
    }
}

class Ident extends AST {
    public final String name;
    public Ident(String name) { this.name = name; }
    @Override
    public String toString(int indent) {
        return  pad(indent) + "Ident(" + name + ")";
    }
}

class IntLit extends AST {
    public final int value;
    public IntLit(int value) { this.value = value; }
    @Override
    public String toString(int indent) {
        return  pad(indent) + "IntLit(" + value + ")";
    }
}

class Index extends AST {
    public final AST array;
    public final AST index;
    public Index(AST array, AST index) { this.array = array; this.index = index; }

    @Override
    public String toString(int indent) {
        return pad(indent) + "Index:\n"
                + pad(indent+1) + "array:\n" + array.toString(indent+2) + "\n"
                + pad(indent+1) + "index:\n" + index.toString(indent+2);
    }
}

class Call extends AST {
    public final String name;
    public final List<AST> args;
    public Call(String name, List<AST> args) { this.name = name; this.args = args; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Call(").append(name).append(")\n");
        for (AST arg : args) sb.append(arg.toString(indent + 1)).append("\n");
        return sb.toString();
    }
}

class BoolLit extends AST {
    public final boolean value;
    public BoolLit(boolean value) { this.value = value; }

    @Override
    public String toString(int indent) {
        return  pad(indent) + "BoolLit(" + value + ")";
    }
}

class Compare extends AST {
    public final String op;    // "==", "!=", "<", ">", "<=", ">="
    public final AST left, right;
    public Compare(String op, AST left, AST right) {  this.op = op; this.left = left; this.right = right; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("Compare(").append(op).append(")\n");
        sb.append(left.toString(indent+1)).append("\n");
        sb.append(right.toString(indent+1)).append("\n");
        return sb.toString();
    }
}

class Quantifier extends AST {
    public final boolean isForall;
    public final String name;
    public final String type;
    public final AST pred;

    public Quantifier(boolean isForall, String name, String type, AST pred) {
        this.isForall = isForall;
        this.name = name;
        this.type = type;
        this.pred = pred;
    }

    @Override
    public String toString(int indent) {
        String sb = isForall ? "Forall" : "Exists";
        return pad(indent) + sb + "(" + name + ": " + type + ")\n"
                + pred.toString(indent + 1);
    }
}

class FormulaRef extends AST {
    public final String name;
    public final List<AST> args;
    public FormulaRef(String name, List<AST> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(pad(indent)).append("FormulaRef(").append(name).append(")\n");
        for (AST a : args) sb.append(a.toString(indent + 1)).append("\n");
        return sb.toString();
    }
}