package semantic;

import lexer.DFA;
import lexer.DFALoader;
import lexer.Lexer;
import lexer.Token;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.AST.Program;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SemanticAnalyzerTest {
    private static DFA dfa;

    @BeforeAll
    static void setup() throws Exception {
        dfa = DFALoader.load("export/");
    }

    private List<String> analyze(String code) {
        Lexer lexer = new Lexer(dfa, code);
        List<Token> tokens = lexer.tokenize();
        Program program = (Program) new Parser(tokens).parseProgram();

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(program);
        return analyzer.getErrors();
    }

    private void assertErrorWithPos(List<String> errors, String expectedMsg) {
        assertFalse(errors.isEmpty(), "Ожидалась ошибка: " + expectedMsg);
        boolean found = errors.stream().anyMatch(e ->
                e.contains(":") && e.contains(expectedMsg)
        );
        assertTrue(found, "Не найдена ошибка с позицией и текстом: " + expectedMsg + " в " + errors);
    }

    @Nested
    class PositiveTests {
        @Test
        void testValidFunction() {
            assertTrue(analyze("f(a: int, b: int) returns r: int { r = a + b; }").isEmpty());
        }

        @Test
        void testValidArray() {
            assertTrue(analyze("f(a: int[], n: int) returns r: int { a[0] = n; r = a[n - 1]; }").isEmpty());
        }

        @Test
        void testFunctionCall() {
            assertTrue(analyze("g(x: int) returns y: int { y = x * 2; } f() returns r: int { r = g(5); }").isEmpty());
        }

        @Test
        void testValidControlFlow() {
            String code = "f(x: int) returns r: int { if (x > 0) { r = 1; } else { r = 2; } while (x > 0) invariant x >= 0 { x = x - 1; } }";
            assertTrue(analyze(code).isEmpty());
        }

        @Test
        void testValidRequiresEnsures() {
            String code = "f(x: int) requires x > 0 returns r: int ensures r >= x { r = x; }";
            assertTrue(analyze(code).isEmpty());
        }
    }

    @Nested
    class NegativeTests {
        @Test
        void testUndeclaredVariable() {
            List<String> errors = analyze("f() returns r: int { r = x; }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "Undeclared variable: x");
        }

        @Test
        void testTypeMismatch() {
            List<String> errors = analyze("f(a: int[]) returns r: int { r = a; }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "Type mismatch in assignment");
        }

        @Test
        void testArrayIndexNotInt() {
            List<String> errors = analyze("f(a: int[], b: int[]) returns r: int { r = a[b]; }");
            assertErrorWithPos(errors, "Array index must be int");
        }

        @Test
        void testIndexNonArray() {
            List<String> errors = analyze("f(a: int) returns r: int { r = a[0]; }");
            assertErrorWithPos(errors, "Cannot index non-array type");
        }

        @Test
        void testCallWrongArgCount() {
            List<String> errors = analyze("g(x: int) returns y: int { y = x; } f() returns r: int { r = g(1, 2); }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "expects 1 arguments, but got 2");
        }

        @Test
        void testCallWrongArgType() {
            List<String> errors = analyze("g(x: int[]) returns y: int { y = 0; } f() returns r: int { r = g(5); }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "expects int[]");
        }

        @Test
        void testUndeclaredFunction() {
            List<String> errors = analyze("f() returns r: int { r = missing_func(); }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "Undeclared function: missing_func");
        }

        @Test
        void testDuplicateParameter() {
            List<String> errors = analyze("f(x: int, x: int) returns r: int { r = x; }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "Parameter 'x' is already defined");
        }

        @Test
        void testDuplicateLocalVar() {
            List<String> errors = analyze("f() returns r: int uses x, x: int { r = 0; }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "Local variable 'x' is already defined");
        }

        @Test
        void testConditionMustBeBool() {
            List<String> errors = analyze("f() returns r: int { if (1) r = 0; }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "If condition must be boolean");
        }

        @Test
        void testWhileConditionMustBeBool() {
            List<String> errors = analyze("f() returns r: int { while (1) { r = 0; } }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "While condition must be boolean");
        }

        @Test
        void testRequiresMustBeBool() {
            List<String> errors = analyze("f() requires 1 returns r: int { r = 0; }");
            assertEquals(1, errors.size());
            assertErrorWithPos(errors, "Requires condition must be boolean");
        }

        @Test
        void testArithmeticOnArray() {
            List<String> errors = analyze("f(a: int[]) returns r: int { r = a + 1; }");
            assertErrorWithPos(errors, "Arithmetic operation '+' requires int");
        }
    }
}