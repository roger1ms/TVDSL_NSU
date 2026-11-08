package parser;

import lexer.Token;
import parser.AST.*;

import java.util.*;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private final List<String> errors = new ArrayList<>();
    // Внутри скобок допустимы и выражения, и условия, и предикаты (контракты).
    // Флаг говорит единственной продукции "(" в parseAtom, что именно разбирать,
    // поэтому пробный разбор с откатом не нужен.
    private boolean predicateMode = false;

    public List<String> getErrors() { return Collections.unmodifiableList(errors); }

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token peek(int k) {
        return pos + k < tokens.size() ? tokens.get(pos + k) : tokens.getLast();
    }

    private Token getAndNext() {
        return tokens.get(pos++);
    }

    private boolean check(String type) {
        return peek().name.equals(type);
    }

    private boolean match(String type) {
        if (check(type)) { pos++; return true; }
        return false;
    }

    private Token expect(String type) {
        if (!check(type)) {
            Token t = peek();
            throw new ParseError("expected " + type + " but got " + t.name
                    + " at " + t.line + ":" + t.col);
        }
        return getAndNext();
    }

    private <T extends AST> T withPos(T node, Token t) {
        node.line = t.line;
        node.col = t.col;
        return node;
    }

    // ============================================================
    //  Expressions
    // ============================================================

    public AST parseAddExpr() {
        AST left = parseMulExpr();
        while (check("PLUS") || check("MINUS")) {
            Token opTok = peek();
            String op = getAndNext().lexeme;
            AST right = parseMulExpr();
            left = withPos(new BinOp(op, left, right), opTok);
        }
        return left;
    }

    public AST parseMulExpr() {
        AST left = parseAtom();
        while (check("STAR") || check("SLASH")) {
            Token opTok = peek();
            String op = getAndNext().lexeme;
            AST right = parseAtom();
            left = withPos(new BinOp(op, left, right), opTok);
        }
        return left;
    }

    public AST parseAtom() {
        Token t = peek();

        if (check("INT")) {
            getAndNext();
            return withPos(new IntLit(Integer.parseInt(t.lexeme)), t);
        }
        if (check("MINUS")) {
            getAndNext();
            return withPos(new UnOp("-", parseAtom()), t);
        }
        if (check("LPAREN")) {
            getAndNext();
            // Единственная продукция для "(". Внутри может быть выражение, условие
            // (requires (x > 0)) или предикат (в контрактах). Разбор самого общего
            // случая здесь, на нижнем уровне арифметики, снимает необходимость
            // пробовать альтернативы и откатываться: `+ - * /`, сравнения и
            // логические операторы продолжаются естественно после ")".
            AST e = predicateMode ? parsePredicate() : parseCondition();
            expect("RPAREN");
            return e;
        }
        if (check("IDENT") || check("LENGTH")) {
            getAndNext();
            if (t.name.equals("LENGTH") && !check("LPAREN")) {
                throw new ParseError("expected LPAREN after length at " + t.line + ":" + t.col);
            }
            if (check("LBRACKET")) {
                getAndNext();
                AST idx = parseAddExpr();
                expect("RBRACKET");
                return withPos(new Index(withPos(new Ident(t.lexeme), t), idx), t);
            }
            if (check("LPAREN")) {
                getAndNext();
                List<AST> args = new ArrayList<>();
                if (!check("RPAREN")) {
                    args.add(parseAddExpr());
                    while (match("COMMA")) args.add(parseAddExpr());
                }
                expect("RPAREN");
                return withPos(new Call(t.lexeme, args), t);
            }
            return withPos(new Ident(t.lexeme), t);
        }
        throw new ParseError("unexpected " + t.name + " at " + t.line + ":" + t.col);
    }

    // ============================================================
    //  Conditions
    // ============================================================

    public AST parseCondition() {
        boolean saved = predicateMode;
        predicateMode = false;
        try {
            return parseImpl();
        } finally {
            predicateMode = saved;
        }
    }

    public AST parseImpl() {
        AST left = parseOrExpr();
        if (check("IMPL")) {
            Token t = peek();
            getAndNext();
            AST right = parseImpl();
            return withPos(new BinOp("->", left, right), t);
        }
        return left;
    }

    public AST parseOrExpr() {
        AST left = parseAndExpr();
        while (check("OR")) {
            Token t = peek();
            getAndNext();
            AST right = parseAndExpr();
            left = withPos(new BinOp("or", left, right), t);
        }
        return left;
    }

    public AST parseAndExpr() {
        AST left = parseNotExpr();
        while (check("AND")) {
            Token t = peek();
            getAndNext();
            AST right = parseNotExpr();
            left = withPos(new BinOp("and", left, right), t);
        }
        return left;
    }

    public AST parseNotExpr() {
        if (check("NOT")) {
            Token t = peek();
            getAndNext();
            AST right = parseNotExpr();
            return withPos(new UnOp("not", right), t);
        }
        return parsePrimary();
    }

    public AST parsePrimary() {
        if (check("TRUE")) {
            Token t = peek();
            getAndNext();
            return withPos(new BoolLit(true), t);
        }
        if (check("FALSE")) {
            Token t = peek();
            getAndNext();
            return withPos(new BoolLit(false), t);
        }
        // Скобки обрабатываются в parseAtom как единственная продукция "(", поэтому
        // здесь отдельный случай с пробным разбором больше не нужен.
        return parseComparison();
    }

    public AST parseComparison() {
        AST left = parseAddExpr();
        if (check("EQ") || check("NEQ") || check("LT")
                || check("GT") || check("LE") || check("GE")) {
            Token t = peek();
            String op = getAndNext().lexeme;
            AST right = parseAddExpr();
            return withPos(new Compare(op, left, right), t);
        }
        return left;
    }

    // ============================================================
    //  Predicates
    // ============================================================

    public AST parsePredicate() {
        boolean saved = predicateMode;
        predicateMode = true;
        try {
            return parsePredImpl();
        } finally {
            predicateMode = saved;
        }
    }

    public AST parsePredImpl() {
        AST left = parsePredOr();
        if (check("IMPL")) {
            Token t = peek();
            getAndNext();
            AST right = parsePredImpl();
            return withPos(new BinOp("->", left, right), t);
        }
        return left;
    }

    public AST parsePredOr() {
        AST left = parsePredAnd();
        while (check("OR")) {
            Token t = peek();
            getAndNext();
            AST right = parsePredAnd();
            left = withPos(new BinOp("or", left, right), t);
        }
        return left;
    }

    public AST parsePredAnd() {
        AST left = parsePredNot();
        while (check("AND")) {
            Token t = peek();
            getAndNext();
            AST right = parsePredNot();
            left = withPos(new BinOp("and", left, right), t);
        }
        return left;
    }

    public AST parsePredNot() {
        if (check("NOT")) {
            Token t = peek();
            getAndNext();
            AST right = parsePredNot();
            return withPos(new UnOp("not", right), t);
        }
        return parsePredPrimary();
    }

    public AST parsePredPrimary() {
        if (check("TRUE")) {
            Token t = peek();
            getAndNext();
            return withPos(new BoolLit(true), t);
        }
        if (check("FALSE")) {
            Token t = peek();
            getAndNext();
            return withPos(new BoolLit(false), t);
        }
        if (check("FORALL") || check("EXISTS")) {
            Token startTok = peek();
            boolean isForall = check("FORALL");
            getAndNext();
            expect("LPAREN");
            Param p = parseParam();
            expect("PIPE");
            AST predicate = parsePredicate();
            expect("RPAREN");
            return withPos(new Quantifier(isForall, p.name, p.typeName, predicate), startTok);
        }
        // Скобки и вызовы обрабатываются общим путём через parseComparison/parseAtom
        // (единственная продукция "("), поэтому пробный разбор с откатом не нужен.
        Token start = peek();
        AST comparison = parseComparison();
        if (comparison instanceof Call call) {
            return withPos(new FormulaRef(call.name, call.args), start);
        }
        return comparison;
    }

    // ============================================================
    //  Statements
    // ============================================================

    public AST parseStatement() {
        if (check("LBRACE")) return parseBlock();
        if (check("IF"))     return parseIf();
        if (check("WHILE"))  return parseWhile();
        if (check("ASSERT")) return parseAssert();
        if (check("ASSUME")) return parseAssume();
        if (check("IDENT"))  return parseAssign();

        Token t = peek();
        throw new ParseError("unexpected " + t.name + " at " + t.line + ":" + t.col);
    }

    public AST parseBlock() {
        Token t = peek();
        expect("LBRACE");
        List<AST> statements = new ArrayList<>();
        while (!check("RBRACE")) {
            statements.add(parseStatement());
        }
        expect("RBRACE");
        return withPos(new Block(statements), t);
    }

    public AST parseIf() {
        Token t = peek();
        expect("IF");
        expect("LPAREN");
        AST cond = parseCondition();
        expect("RPAREN");
        AST stmtIf = parseStatement(), stmtElse = null;

        if (check("ELSE")) {
            getAndNext();
            stmtElse = parseStatement();
        }
        return withPos(new If(cond, stmtIf, stmtElse), t);
    }

    public AST parseWhile() {
        Token t = peek();
        expect("WHILE");
        expect("LPAREN");
        AST cond = parseCondition(), predicate = null;
        expect("RPAREN");

        if (check("INVARIANT")) {
            getAndNext();
            predicate = parsePredicate();
        }
        AST stmtWhile = parseStatement();
        return withPos(new While(cond, predicate, stmtWhile), t);
    }

    public AST parseAssert() {
        Token t = peek();
        expect("ASSERT");
        AST predicate = parsePredicate();
        expect("SEMI");
        return withPos(new Assert(predicate), t);
    }

    public AST parseAssume() {
        Token t = peek();
        expect("ASSUME");
        AST predicate = parsePredicate();
        expect("SEMI");
        return withPos(new Assume(predicate), t);
    }

    public AST parseAssign() {
        Token id = expect("IDENT");
        AST target = withPos(new Ident(id.lexeme), id);

        if (check("COMMA")) {
            List<Ident> targets = new ArrayList<>();
            targets.add((Ident) target);
            while (match("COMMA")) {
                Token next = expect("IDENT");
                targets.add(withPos(new Ident(next.lexeme), next));
            }
            expect("ASSIGN");
            AST rhs = parseAtom();
            if (!(rhs instanceof Call call)) {
                throw new ParseError("expected function call in tuple assignment at " + id.line + ":" + id.col);
            }
            expect("SEMI");
            return withPos(new TupleAssign(targets, call), id);
        }

        if (check("LBRACKET")) {
            getAndNext();
            AST idx = parseAddExpr();
            expect("RBRACKET");
            target = withPos(new Index(target, idx), id);
        }

        expect("ASSIGN");
        AST value = parseAddExpr();
        expect("SEMI");
        return withPos(new Assign(target, value), id);
    }

    // ============================================================
    //  Functions
    // ============================================================

    public AST parseProgram() {
        List<AST> items = new ArrayList<>();
        while (!check("EOF")) {
            try {
                items.add(isFormulaDefinition() ? parseFormula() : parseFunction());
            } catch (ParseError e) {
                errors.add(e.getMessage());
                System.err.println("Syntax Error: " + e.getMessage());
                int braces = 0;
                boolean skipped = false;

                while (!check("EOF")) {
                    if (check("LBRACE")) braces++;
                    if (check("RBRACE")) braces--;

                    getAndNext();
                    skipped = true;

                    if (braces <= 0 && check("IDENT")) {
                        break;
                    }
                }

                if (!skipped && !check("EOF")) {
                    getAndNext();
                }
            }
        }
        return new Program(items);
    }

    private boolean isFormulaDefinition() {
        if (!check("IDENT") || !peek(1).name.equals("LPAREN")) return false;
        int depth = 0;
        for (int i = pos + 1; i < tokens.size(); i++) {
            String name = tokens.get(i).name;
            if (name.equals("LPAREN")) depth++;
            else if (name.equals("RPAREN") && --depth == 0) {
                return i + 1 < tokens.size() && tokens.get(i + 1).name.equals("FATARROW");
            }
        }
        return false;
    }

    public AST parseFormula() {
        Token start = expect("IDENT");
        expect("LPAREN");
        List<Param> params = parseParamList();
        expect("RPAREN");
        expect("FATARROW");
        return withPos(new Formula(start.lexeme, params, parsePredicate()), start);
    }

    public AST parseFunction() {
        Token t = expect("IDENT");
        expect("LPAREN");
        List<Param> params = parseParamList();
        expect("RPAREN");
        AST requires = match("REQUIRES") ? parsePredicate() : null;
        expect("RETURNS");
        List<Param> returns = parseReturnList();
        AST ensures = match("ENSURES") ? parsePredicate() : null;
        List<LocalVar> uses = new ArrayList<>();
        if (match("USES")) {
            uses = parseLocalVarList();
        }
        AST body = parseStatement();

        return withPos(new Function(t.lexeme, params, returns, requires, ensures, uses, body), t);
    }

    public List<Param> parseParamList() {
        List<Param> params = new ArrayList<>();
        if (check("RPAREN")) return params;
        params.add(parseParam());
        while (match("COMMA")) {
            params.add(parseParam());
        }
        return params;
    }

    public List<Param> parseReturnList() {
        List<Param> params = new ArrayList<>();
        params.add(parseParam());
        while (match("COMMA")) {
            params.add(parseParam());
        }
        return params;
    }

    public Param parseParam() {
        Token t = expect("IDENT");
        expect("COLON");
        return withPos(new Param(t.lexeme, parseType()), t);
    }

    public List<LocalVar> parseLocalVarList() {
        List<LocalVar> vars = new ArrayList<>();
        vars.add(parseLocalVar());
        while (match("COMMA")) {
            vars.add(parseLocalVar());
        }
        return vars;
    }

    public LocalVar parseLocalVar() {
        Token id = expect("IDENT");
        String type = match("COLON") ? parseType() : null;
        return withPos(new LocalVar(id.lexeme, type), id);
    }

    public String parseType() {
        Token t = expect("IDENT");
        if (check("LBRACKET")) {
            getAndNext();
            expect("RBRACKET");
            return t.lexeme + "[]";
        }
        return t.lexeme;
    }
}
