package lexer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {

    private static DFA dfa;

    @BeforeAll
    static void setup() throws Exception {
        dfa = DFALoader.load("export/");
    }

    private List<Token> tokenize(String input) {
        return new Lexer(dfa, input).tokenize();
    }

    @Nested
    class BasicTokens {
        @Test
        void testEmpty() {
            List<Token> tokens = tokenize("");
            assertEquals(1, tokens.size());
            assertEquals("EOF", tokens.get(0).name);
        }

        @Test
        void testOnlySpaces() {
            List<Token> tokens = tokenize("   \t\n\r ");
            assertEquals(1, tokens.size());
            assertEquals("EOF", tokens.get(0).name);
        }

        @Test
        void testSingleIdentifier() {
            List<Token> tokens = tokenize("foo");
            assertEquals(2, tokens.size());
            assertEquals("IDENT", tokens.get(0).name);
            assertEquals("foo", tokens.get(0).lexeme);
        }

        @Test
        void testIdentifierWithUnderscore() {
            List<Token> tokens = tokenize("_foo_bar1");
            assertEquals("IDENT", tokens.get(0).name);
            assertEquals("_foo_bar1", tokens.get(0).lexeme);
        }

        @Test
        void testIntegerZero() {
            List<Token> tokens = tokenize("0");
            assertEquals("INT", tokens.get(0).name);
            assertEquals("0", tokens.get(0).lexeme);
        }

        @Test
        void testIntegerMulti() {
            List<Token> tokens = tokenize("12345");
            assertEquals("INT", tokens.get(0).name);
            assertEquals("12345", tokens.get(0).lexeme);
        }
    }

    @Nested
    class KeywordsAndIdentifiers {
        @ParameterizedTest
        @ValueSource(strings = {"function", "returns", "while", "if", "else", "assert", "assume", "invariant", "length", "forall", "exists", "and", "or", "not"})
        void testKeywords(String kw) {
            List<Token> tokens = tokenize(kw);
            // Имя токена для ключевых слов — это сам keyword в ВЕРХНЕМ регистре
            assertEquals(kw.toUpperCase(), tokens.get(0).name);
        }

        @ParameterizedTest
        @ValueSource(strings = {"iff", "functions", "elsewhere", "while1", "if_"})
        void testKeywordPrefixesAreIdent(String ident) {
            List<Token> tokens = tokenize(ident);
            assertEquals("IDENT", tokens.get(0).name);
        }
    }

    @Nested
    class Operators {
        @ParameterizedTest
        @ValueSource(strings = {"(", ")", "{", "}", "[", "]", ",", ";", ":", "+", "-", "*", "/", "<", ">", "="})
        void testSingleCharOperators(String op) {
            List<Token> tokens = tokenize(op);
            assertEquals(2, tokens.size()); // op + EOF
            assertNotEquals("ERROR", tokens.get(0).name);
            assertEquals(op, tokens.get(0).lexeme);
        }

        @ParameterizedTest
        @ValueSource(strings = {"==", "!=", "<=", ">=", "->", "=>"})
        void testMultiCharOperators(String op) {
            List<Token> tokens = tokenize(op);
            assertEquals(2, tokens.size());
            assertNotEquals("ERROR", tokens.get(0).name);
            assertEquals(op, tokens.get(0).lexeme);
        }
    }

    @Nested
    class CommentsAndWhitespace {
        @Test
        void testCommentSkip() {
            List<Token> tokens = tokenize("foo // this is a comment\n bar");
            assertEquals(3, tokens.size()); // foo, bar, EOF
            assertEquals("foo", tokens.get(0).lexeme);
            assertEquals("bar", tokens.get(1).lexeme);
        }

        @Test
        void testComplexWhitespace() {
            List<Token> tokens = tokenize("  \t  if \r\n  (  x )  ");
            assertEquals(5, tokens.size()); // if, (, x, ), EOF
        }
    }

    @Nested
    class Positions {
        @Test
        void testSingleLine() {
            List<Token> tokens = tokenize("if x = 1;");
            assertEquals(1, tokens.get(0).line); // if
            assertEquals(1, tokens.get(0).col);

            assertEquals(1, tokens.get(1).line); // x
            assertEquals(4, tokens.get(1).col);
        }

        @Test
        void testMultiLine() {
            List<Token> tokens = tokenize("if\nx");
            assertEquals(1, tokens.get(0).line); // if
            assertEquals(2, tokens.get(1).line); // x
            assertEquals(1, tokens.get(1).col);
        }
    }

    @Nested
    class ErrorsAndTrap {
        @Test
        void testInvalidChar() {
            List<Token> tokens = tokenize("@");
            assertEquals(2, tokens.size()); // ERROR, EOF
            assertEquals("ERROR", tokens.get(0).name);
        }

        @Test
        void testInvalidCharInMiddle() {
            List<Token> tokens = tokenize("if @ x");
            // if, ERROR, x, EOF
            assertEquals(4, tokens.size());
            assertEquals("ERROR", tokens.get(1).name);
        }

        @Test
        void testNonAscii() {
            List<Token> tokens = tokenize("ы");
            assertEquals("ERROR", tokens.get(0).name);
        }

        @Test
        void testTrapRecovery() {
            // Лексер должен пропустить один плохой символ и продолжить дальше
            List<Token> tokens = tokenize("if @ returns");
            assertEquals(4, tokens.size()); // if, ERROR, returns, EOF
            assertEquals("returns", tokens.get(2).lexeme);
        }
    }
}