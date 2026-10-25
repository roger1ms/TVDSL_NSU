package parser;

import lexer.DFA;
import lexer.DFALoader;
import lexer.Lexer;
import lexer.Token;
import parser.AST.*;
import semantic.SemanticAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java parser.Main <input.funny>");
            return;
        }

        String filePath = args[0];
        try {
            String sourceCode = Files.readString(Path.of(filePath));

            DFA dfa = DFALoader.load("export/");
            Lexer lexer = new Lexer(dfa, sourceCode);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            AST ast = parser.parseProgram();

            System.out.println("=== AST (Before Semantic Analysis) ===");
            System.out.println(ast.toString(0));

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze((Program) ast);

            System.out.println("\n=== AST (After Semantic Analysis) ===");
            System.out.println(ast.toString(0));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}