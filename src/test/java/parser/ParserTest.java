package parser;

import lexer.DFA;
import lexer.DFALoader;
import lexer.Lexer;
import lexer.Token;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    private static DFA dfa;

    @BeforeAll
    static void setup() throws Exception {
        dfa = DFALoader.load("export/");
    }

    private AST parse(String input) {
        Lexer lexer = new Lexer(dfa, input);
        List<Token> tokens = lexer.tokenize();
        return new Parser(tokens).parseProgram();
    }

    private Function parseFunc(String code) {
        return (Function) ((Program) parse(code)).items.get(0);
    }

    private AST parseSingle(String input) {
        Lexer lexer = new Lexer(dfa, input);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parseFunction(); // Парсим только одну функцию, без Panic Mode
    }
    // ==========================================================
    // 1. ВЫРАЖЕНИЯ И ПРИОРИТЕТЫ (9 тестов)
    // ==========================================================
    @Nested
    class ExpressionTests {

        @Test
        void testPrecedenceMulOverAdd() {
            String code = "f() returns r: int { r = 2 + 3 * 4; }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            BinOp top = (BinOp) assign.value;
            assertEquals("+", top.op);
            assertInstanceOf(IntLit.class, top.left);
            assertInstanceOf(BinOp.class, top.right);
            assertEquals("*", ((BinOp) top.right).op);
        }

        @Test
        void testPrecedenceParentheses() {
            String code = "f() returns r: int { r = (2 + 3) * 4; }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            BinOp top = (BinOp) assign.value;
            assertEquals("*", top.op);
            assertInstanceOf(BinOp.class, top.left);
            assertEquals("+", ((BinOp) top.left).op);
        }

        @Test
        void testLeftAssociativityMinus() {
            String code = "f() returns r: int { r = 10 - 3 - 2; }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            BinOp top = (BinOp) assign.value;
            assertEquals("-", top.op);
            assertInstanceOf(BinOp.class, top.left); // (10 - 3) - 2
            assertEquals("-", ((BinOp) top.left).op);
            assertInstanceOf(IntLit.class, top.right);
        }

        @Test
        void testLeftAssociativityDiv() {
            String code = "f() returns r: int { r = 100 / 10 / 2; }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            BinOp top = (BinOp) assign.value;
            assertEquals("/", top.op);
            assertInstanceOf(BinOp.class, top.left);
            assertEquals("/", ((BinOp) top.left).op);
        }

        @Test
        void testUnaryMinusBinding() {
            String code = "f() returns r: int { r = -a[0]; }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            UnOp top = (UnOp) assign.value;
            assertEquals("-", top.op);
            assertInstanceOf(Index.class, top.operand); // - (a[0]), а не (-a)[0]
        }

        @Test
        void testDoubleUnaryMinus() {
            String code = "f() returns r: int { r = - - 5; }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            UnOp outer = (UnOp) assign.value;
            assertInstanceOf(UnOp.class, outer.operand);
        }

        @Test
        void testNestedCallInsideIndex() {
            String code = "f() returns r: int { r = a[g(x)]; }";
            assertDoesNotThrow(() -> parse(code));
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            Index idx = (Index) assign.value;
            assertInstanceOf(Ident.class, idx.array);
            assertInstanceOf(Call.class, idx.index);
        }

        @Test
        void testFunctionCallWithMultipleArgs() {
            String code = "f() returns r: int { r = add(1, 2, 3); }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            Call call = (Call) assign.value;
            assertEquals("add", call.name);
            assertEquals(3, call.args.size());
        }

        @Test
        void testEmptyFunctionCall() {
            String code = "f() returns r: int { r = get(); }";
            Assign assign = (Assign) ((Block) parseFunc(code).body).stmts.get(0);
            Call call = (Call) assign.value;
            assertTrue(call.args.isEmpty());
        }
    }

    // ==========================================================
    // 2. УСЛОВИЯ И ПРЕДИКАТЫ (6 тестов)
    // ==========================================================
    @Nested
    class ConditionTests {

        @Test
        void testImplicationRightAssociative() {
            String code = "f(x: int) requires x>0 -> x>1 -> x>2 returns r: int { r = x; }";
            BinOp impl = (BinOp) parseFunc(code).requires;
            assertEquals("->", impl.op);
            assertInstanceOf(BinOp.class, impl.right); // a -> (b -> c)
            assertEquals("->", ((BinOp) impl.right).op);
        }

        @Test
        void testAndOrPrecedence() {
            String code = "f(x: int) requires x>0 or x<10 and x!=5 returns r: int { r = x; }";
            BinOp or = (BinOp) parseFunc(code).requires;
            assertEquals("or", or.op);
            assertInstanceOf(BinOp.class, or.right); // (x>0) or ((x<10) and (x!=5))
            assertEquals("and", ((BinOp) or.right).op);
        }

        @Test
        void testNotBinding() {
            String code = "f(x: int) requires not x>0 and x<5 returns r: int { r = x; }";
            BinOp and = (BinOp) parseFunc(code).requires;
            assertEquals("and", and.op);
            assertInstanceOf(UnOp.class, and.left); // (not x>0) and (x<5)
            assertEquals("not", ((UnOp) and.left).op);
        }

        @Test
        void testNotWithComparison() {
            // Убрали скобки, проверяем not с простым сравнением
            String code = "f(x: int) requires not x>0 returns r: int { r = x; }";
            UnOp not = (UnOp) parseFunc(code).requires;
            assertEquals("not", not.op);
            assertInstanceOf(Compare.class, not.operand);
        }

        @Test
        void testComparisonInCondition() {
            String code = "f(x: int) returns r: int { if (x >= 0) r = 1; else r = 2; }";
            Block body = (Block) parseFunc(code).body;
            If ifStmt = (If) body.stmts.get(0);
            Compare cmp = (Compare) ifStmt.cond;
            assertEquals(">=", cmp.op);
        }

        @Test
        void testConditionTrueFalse() {
            String code = "f() returns r: int { if (true) r = 1; else r = 2; }";
            Block body = (Block) parseFunc(code).body;
            If ifStmt = (If) body.stmts.get(0);
            assertInstanceOf(BoolLit.class, ifStmt.cond);
            assertTrue(((BoolLit) ifStmt.cond).value);
        }
    }

    // ==========================================================
    // 3. КВАНТОРЫ (4 теста)
    // ==========================================================
    @Nested
    class QuantifierTests {

        @Test
        void testForall() {
            String code = "f(n: int) requires forall (i: int | i > 0 and i < n) returns r: int { r = 0; }";
            Quantifier q = (Quantifier) parseFunc(code).requires;
            assertTrue(q.isForall);
            assertEquals("i", q.name);
            assertEquals("int", q.type);
            assertInstanceOf(BinOp.class, q.pred);
        }

        @Test
        void testExists() {
            String code = "f(a: int[], n: int) requires exists (i: int | i >= 0 and a[i] == 0) returns r: int { r = 0; }";
            Quantifier q = (Quantifier) parseFunc(code).requires;
            assertFalse(q.isForall);
            assertEquals("i", q.name);
        }

        @Test
        void testNestedQuantifiers() {
            String code = "f(n: int) requires forall (i: int | exists (j: int | i < j)) returns r: int { r = 0; }";
            Quantifier outer = (Quantifier) parseFunc(code).requires;
            assertTrue(outer.isForall);
            assertInstanceOf(Quantifier.class, outer.pred);
            Quantifier inner = (Quantifier) outer.pred;
            assertFalse(inner.isForall);
        }

        @Test
        void testQuantifierWithComplexPredicate() {
            String code = "f(a: int[], n: int) requires forall (i: int | i >= 0 and i < n -> a[i] > 0) returns r: int { r = 0; }";
            Quantifier q = (Quantifier) parseFunc(code).requires;
            assertInstanceOf(BinOp.class, q.pred);
            assertEquals("->", ((BinOp) q.pred).op);
        }
    }

    // ==========================================================
    // 4. ОПЕРАТОРЫ (7 тестов)
    // ==========================================================
    @Nested
    class StatementTests {

        @Test
        void testDanglingElse() {
            String code = "f() returns r: int { if (1 > 0) if (2 > 0) r = 1; else r = 2; }";
            Block body = (Block) parseFunc(code).body;
            If outer = (If) body.stmts.get(0);
            assertNull(outer.else_); // У внешнего if нет else
            If inner = (If) outer.then;
            assertNotNull(inner.else_); // У внутреннего есть
        }

        @Test
        void testEmptyBlock() {
            String code = "f() returns r: int { { } }";
            Block outer = (Block) parseFunc(code).body;
            Block empty = (Block) outer.stmts.get(0);
            assertTrue(empty.stmts.isEmpty());
        }

        @Test
        void testWhileWithInvariant() {
            String code = "f(x: int) returns r: int { while (x > 0) invariant x >= 0 { x = x - 1; } r = 0; }";
            Block body = (Block) parseFunc(code).body;
            While w = (While) body.stmts.get(0);
            assertNotNull(w.invariant);
            assertInstanceOf(Block.class, w.body);
        }

        @Test
        void testWhileWithoutInvariant() {
            String code = "f(x: int) returns r: int { while (x > 0) { x = x - 1; } r = 0; }";
            Block body = (Block) parseFunc(code).body;
            While w = (While) body.stmts.get(0);
            assertNull(w.invariant);
        }

        @Test
        void testArrayAssignment() {
            String code = "f(a: int[]) returns r: int { a[0] = 42; r = 0; }";
            Block body = (Block) parseFunc(code).body;
            Assign assign = (Assign) body.stmts.get(0);
            assertInstanceOf(Index.class, assign.target);
        }

        @Test
        void testMultipleAssignments() {
            String code = "f() returns r: int { r = 1; r = 2; r = 3; }";
            Block body = (Block) parseFunc(code).body;
            assertEquals(3, body.stmts.size());
            assertTrue(body.stmts.stream().allMatch(s -> s instanceof Assign));
        }

        @Test
        void testAssertAndAssume() {
            String code = "f(x: int) returns r: int { assert x > 0; assume x < 100; r = x; }";
            Block body = (Block) parseFunc(code).body;
            assertInstanceOf(Assert.class, body.stmts.get(0));
            assertInstanceOf(Assume.class, body.stmts.get(1));
        }
    }

    // ==========================================================
    // 5. ФУНКЦИИ (5 тестов)
    // ==========================================================
    @Nested
    class FunctionTests {

        @Test
        void testComplexSignature() {
            String code = "f(a: int[], b: int) requires a!=0 returns r: int, s: int[] ensures r>0 uses x, y: int { r = 1; s = a; }";
            Function f = parseFunc(code);
            assertEquals(2, f.params.size());
            assertEquals(2, f.returns.size());
            assertEquals("int[]", f.params.get(0).type);
            assertEquals("int[]", f.returns.get(1).type);
            assertEquals(2, f.uses.size());
            assertNull(f.uses.get(0).type);
            assertEquals("int", f.uses.get(1).type);
        }

        @Test
        void testMultipleFunctions() {
            String code = "f() returns r: int { r = 1; } g() returns r: int { r = 2; }";
            Program p = (Program) parse(code);
            assertEquals(2, p.items.size());
            assertEquals("f", ((Function) p.items.get(0)).name);
            assertEquals("g", ((Function) p.items.get(1)).name);
        }

        @Test
        void testNoRequiresEnsuresUses() {
            String code = "f() returns r: int { r = 0; }";
            Function f = parseFunc(code);
            assertNull(f.requires);
            assertNull(f.ensures);
            assertTrue(f.uses.isEmpty());
        }

        @Test
        void testEmptyProgram() {
            String code = "";
            Program p = (Program) parse(code);
            assertTrue(p.items.isEmpty());
        }

        @Test
        void testParamAndReturnTypes() {
            String code = "f(x: int[]) returns a: int, b: int[] { a = 1; b = x; }";
            Function f = parseFunc(code);
            assertEquals("int[]", f.params.get(0).type);
            assertEquals("int", f.returns.get(0).type);
            assertEquals("int[]", f.returns.get(1).type);
        }
    }

    // ==========================================================
    // 6. ОБРАБОТКА ОШИБОК (Fail-Fast)
    // ==========================================================
    @Nested
    class ErrorTests {

        // Вспомогательный метод: парсим ОДНУ функцию (без Panic Mode)
        // Если в ней ошибка, она вылетит как ParseError.
        private void parseSingleFunction(String code) {
            Lexer lexer = new Lexer(dfa, code);
            Parser parser = new Parser(lexer.tokenize());
            parser.parseFunction(); // Вызываем напрямую, минуя parseProgram
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "f(x: int { r = 1; }",
                "f() returns r: int { r = 1 }",
                "f() returns r: int { r = 1 + ; }",
                "f() returns r: int { if 1 > 0 r = 1; }",
                "f() returns r: int { while (1 > 0) invariant { r = 1; } }",
                "f() returns r: int { assert 1 > 0 }",
                "f(x: int) requires returns r: int { r = 1; }",
                "f(x: ) returns r: int { r = 1; }",
                "f() returns r: int { r = a[]; }",
                "f() returns r: int { r = (1 + 2; }",
                "f() { r = 1; }",
                "f() returns r { r = 1; }",
                "function f() returns r: int { r = 1; }"
        })
        void testSyntaxErrors(String badCode) {
            ParseError e = assertThrows(ParseError.class, () -> parseSingleFunction(badCode),
                    "Должно упасть на коде: " + badCode);
            assertTrue(e.getMessage().contains(":"), "Ошибка должна содержать line:col");
        }

        @Test
        void testErrorPositionAccuracy() {
            String code = "f() returns r: int { r = 1 + ; }";
            ParseError e = assertThrows(ParseError.class, () -> parseSingleFunction(code));
            assertTrue(e.getMessage().contains("1:"), "Должна быть указана строка 1: " + e.getMessage());
        }

        @Test
        void testErrorOnEOF() {
            String code = "f(";
            assertThrows(ParseError.class, () -> parseSingleFunction(code));
        }

        @Test
        void testErrorOnBadToken() {
            String code = "f() returns r: int { r = @; }"; // @ невалидный символ
            assertThrows(RuntimeException.class, () -> parseSingleFunction(code));
        }
    }

    @Nested
    class RecoveryTests {

        @Test
        void testRecoverNextFunction() {
            String code = "f() returns r: int { r = 1 + ; } g() returns r: int { r = 2; }";
            AST ast = parse(code); // Вызываем parseProgram (с Panic Mode)
            Program p = (Program) ast;

            // Должны прочитать хотя бы вторую функцию
            assertEquals(1, p.items.size(), "Должна прочитаться вторая функция");
            Function g = (Function) p.items.get(0);
            assertEquals("g", g.name);
        }
    }
}