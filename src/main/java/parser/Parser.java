package parser;

import lexer.Token;
import java.util.*;

public class Parser {
    // ============================================================
    //  Helpers
    // ============================================================
    private List<Token> tokens;
    private int pos;

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

    // ============================================================
    //  Expressions
    // ============================================================

    // addExpr = mulExpr { ("+" | "-") mulExpr }
    public AST parseAddExpr() {
        AST left = parseMulExpr();

        while (check("PLUS") || check("MINUS")) {
            String op = getAndNext().lexeme;
            AST right = parseMulExpr();
            left = new BinOp(op, left, right);
        }

        return left;
    }

    // mulExpr = atom { ("*" | "/") atom }
    public AST parseMulExpr() {
        AST left = parseAtom();

        while (check("STAR") || check("SLASH")) {
            String op = getAndNext().lexeme;
            AST right = parseAtom();
            left = new BinOp(op, left, right);
        }

        return left;
    }

    // atom = INT | IDENT [ "(" args ")" | "[" expr "]" ] | "-" atom | "(" expr ")"
    public AST parseAtom() {
        Token t = peek();

        if (check("INT")) {
            getAndNext();
            return new IntLit(Integer.parseInt(t.lexeme));
        }
        if (check("MINUS")) {
            getAndNext();
            return new UnOp("-", parseAtom());
        }
        if (check("LPAREN")) {
            getAndNext();
            AST e = parseAddExpr();
            expect("RPAREN");
            return e;
        }
        if (check("IDENT")) {
            getAndNext();
            if (check("LBRACKET")) {
                getAndNext();
                AST idx = parseAddExpr();
                expect("RBRACKET");
                return new Index(new Ident(t.lexeme), idx);
            }
            if (check("LPAREN")) {
                getAndNext();
                List<AST> args = new ArrayList<>();
                if (!check("RPAREN")) {
                    args.add(parseAddExpr());
                    while (match("COMMA")) args.add(parseAddExpr());
                }
                expect("RPAREN");
                return new Call(t.lexeme, args);
            }
            return new Ident(t.lexeme);
        }
        throw new ParseError("unexpected " + t.name + " at " + t.line + ":" + t.col);
    }

    // ============================================================
    //  Conditions
    // ============================================================

    // condition = impl
    public AST parseCondition() {
        return parseImpl();
    }

    // impl = orExpr [ "->" impl ]
    public AST parseImpl() {
        AST left = parseOrExpr();
        if (match("IMPL")) {
            AST right = parseImpl();
            return new BinOp("->", left, right);
        }
        return left;
    }

    // orExpr = andExpr { "or" andExpr }
    public AST parseOrExpr() {
        AST left = parseAndExpr();
        while (match("OR")) {
            AST right = parseAndExpr();
            left = new BinOp("or", left, right);
        }
        return left;
    }

    // andExpr = notExpr { "and" notExpr }
    public AST parseAndExpr() {
        AST left = parseNotExpr();
        while (match("AND")) {
            AST right = parseNotExpr();
            left = new BinOp("and", left, right);
        }
        return left;
    }

    // notExpr = "not" notExpr | primary
    public AST parseNotExpr() {
        if (match("NOT")) {
            AST right = parseNotExpr();
            return new UnOp("not", right);
        }
        return parsePrimary();
    }

    // primary = "true" | "false" | comparison | "(" condition ")"
    public AST parsePrimary() {
        if (check("TRUE")) {
            getAndNext();
            return new BoolLit(true);
        }
        if (check("FALSE")) {
            getAndNext();
            return new BoolLit(false);
        }
        if (check("LPAREN")) {
            getAndNext();
            AST cond = parseCondition();
            expect("RPAREN");
            return cond;
        }
        return parseComparison();
    }

    // comparison = expr [ ("==" | "!=" | "<" | ">" | "<=" | ">=") expr ]
    public AST parseComparison() {
        AST left = parseAddExpr();

        if (check("EQ") || check("NEQ") || check("LT")
                || check("GT") || check("LE") || check("GE")) {
            String op = getAndNext().lexeme;
            AST right = parseAddExpr();
            return new Compare(op, left, right);
        }
        return left;
    }

    // ============================================================
    //  Predicates
    // ============================================================

    // predicate = predImpl
    public AST parsePredicate() {
        return parsePredImpl();
    }

    // predImpl = predOr [ "->" predImpl ]
    public AST parsePredImpl() {
        AST left = parsePredOr();
        if (match("IMPL")) {
            AST right = parsePredImpl();
            return new BinOp("->", left, right);
        }
        return left;
    }

    // predOr = predAnd { "or" predAnd }
    public AST parsePredOr() {
        AST left = parsePredAnd();
        while (match("OR")) {
            AST right = parsePredAnd();
            left = new BinOp("or", left, right);
        }
        return left;
    }

    // predAnd = predNot { "and" predNot }
    public AST parsePredAnd() {
        AST left = parsePredNot();
        while (match("AND")) {
            AST right = parsePredNot();
            left = new BinOp("and", left, right);
        }
        return left;
    }

    // predNot = "not" predNot | predPrimary
    public AST parsePredNot() {
        if (match("NOT")) {
            AST right = parsePredNot();
            return new UnOp("not", right);
        }
        return parsePredPrimary();
    }

    // predPrimary = quantifier | formulaRef | "true" | "false" | comparison | "(" predicate ")"
    public AST parsePredPrimary() {
        if (check("TRUE")) {
            getAndNext();
            return new BoolLit(true);
        }
        if (check("FALSE")) {
            getAndNext();
            return new BoolLit(false);
        }
        if (check("FORALL") || check("EXISTS")) {
            boolean isForall = check("FORALL");
            getAndNext();
            expect("LPAREN");
            Param p = parseParam();
            expect("PIPE");
            AST predicate = parsePredicate();
            expect("RPAREN");
            return new Quantifier(isForall, p.name, p.type, predicate);
        }
        if (check("IDENT") && peek(1).name.equals("LPAREN")) {
            Token id = getAndNext();
            getAndNext();
            List<AST> args = new ArrayList<>();
            if (!check("RPAREN")) {
                args.add(parseAddExpr());
                while (match("COMMA")) args.add(parseAddExpr());
            }
            expect("RPAREN");
            return new FormulaRef(id.lexeme, args);
        }
        return parseComparison();
    }

    // ============================================================
    //  Statements
    // ============================================================

    // statement = block | if | while | assert | assume | assign
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

    // block = "{" { statement } "}"
    public AST parseBlock() {
        expect("LBRACE");
        List<AST> statements = new ArrayList<>();
        while (!check("RBRACE")) {
            statements.add(parseStatement());
        }
        expect("RBRACE");
        return new Block(statements);
    }

    // if = "if" "(" condition ")" statement [ "else" statement ]
    public AST parseIf() {
        expect("IF");
        expect("LPAREN");
        AST cond = parseCondition();
        expect("RPAREN");
        AST stmtIf = parseStatement(), stmtElse = null;

        if (check("ELSE")) {
            getAndNext();
            stmtElse = parseStatement();
        }
        return new If(cond, stmtIf, stmtElse);
    }

    // while = "while" "(" condition ")" [ "invariant" predicate ] statement
    public AST parseWhile() {
        expect("WHILE");
        expect("LPAREN");
        AST cond = parseCondition(), predicate = null;
        expect("RPAREN");

        if (check("INVARIANT")) {
            getAndNext();
            predicate = parsePredicate();
        }
        AST stmtWhile = parseStatement();
        return new While(cond, predicate, stmtWhile);
    }

    // assert = "assert" predicate ";"
    public AST parseAssert() {
        expect("ASSERT");
        AST predicate = parsePredicate();
        expect("SEMI");
        return new Assert(predicate);
    }

    // assume = "assume" predicate ";"
    public AST parseAssume() {
        expect("ASSUME");
        AST predicate = parsePredicate();
        expect("SEMI");
        return new Assume(predicate);
    }

    // assign = IDENT [ "[" expr "]" ] "=" expr ";"
    public AST parseAssign() {
        Token id = expect("IDENT");
        AST target = new Ident(id.lexeme);

        if (check("LBRACKET")) {
            getAndNext();
            AST idx = parseAddExpr();
            expect("RBRACKET");
            target = new Index(target, idx);
        }

        expect("ASSIGN");
        AST value = parseAddExpr();
        expect("SEMI");
        return new Assign(target, value);
    }

    // ============================================================
    //  Functions (top level)
    // ============================================================

    // program = { function }
    public AST parseProgram() {
        List<AST> items = new ArrayList<>();
        while (!check("EOF")) {
            try {
                items.add(parseFunction());
            } catch (ParseError e) {
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

    // function = IDENT "(" paramList ")" [requires] "returns" returnList [ensures] [uses] statement
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

        return new Function(t.lexeme, params, returns, requires, ensures, uses, body);
    }

    // paramList = [ param { "," param } ]
    public List<Param> parseParamList() {
        List<Param> params = new ArrayList<>();
        if (check("RPAREN")) return params;
        params.add(parseParam());
        while (match("COMMA")) {
            params.add(parseParam());
        }
        return params;
    }

    // returnList = param { "," param }
    public List<Param> parseReturnList() {
        List<Param> params = new ArrayList<>();
        params.add(parseParam());
        while (match("COMMA")) {
            params.add(parseParam());
        }
        return params;
    }

    // param = IDENT ":" type
    public Param parseParam() {
        Token t = expect("IDENT");
        expect("COLON");
        return new Param(t.lexeme, parseType());
    }

    // localVarList = localVar { "," localVar }
    public List<LocalVar> parseLocalVarList() {
        List<LocalVar> vars = new ArrayList<>();
        vars.add(parseLocalVar());
        while (match("COMMA")) {
            vars.add(parseLocalVar());
        }
        return vars;
    }

    // localVar = IDENT [ ":" type ]
    public LocalVar parseLocalVar() {
        Token id = expect("IDENT");
        String type = match("COLON") ? parseType() : null;
        return new LocalVar(id.lexeme, type);
    }

    // type = IDENT [ "[" "]" ]
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