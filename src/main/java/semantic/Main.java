package semantic;

import lexer.DFA;
import lexer.DFALoader;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.AST.Program;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String sourceCode;
        if (args.length > 0) {
            try {
                sourceCode = Files.readString(Path.of(args[0]));
            } catch (Exception e) {
                System.err.println("Ошибка чтения файла: " + e.getMessage());
                return;
            }
        } else {
            sourceCode = "f(x: int) returns r: int uses b { r = x + 1; b = r + 1; }";
            System.out.println("Тестовый код:\n" + sourceCode + "\n");
        }

        try {
            DFA dfa = DFALoader.load("export/");
            Lexer lexer = new Lexer(dfa, sourceCode);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            Program program = (Program) parser.parseProgram();

            System.out.println("=== AST (Before) ===");
            System.out.println(program.toString(0));

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(program);

            System.out.println("\n=== Semantic Errors ===");
            List<String> errors = analyzer.getErrors();
            if (errors.isEmpty()) {
                System.out.println("No errors found.");
            } else {
                for (String err : errors) {
                    System.out.println("  " + err);
                }
            }

            System.out.println("\n=== AST (Annotated) ===");
            System.out.println(program.toString(0));

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}