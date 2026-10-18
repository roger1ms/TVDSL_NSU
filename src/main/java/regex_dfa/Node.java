package regex_dfa;
import java.util.HashSet;
import java.util.Set;

public abstract class Node {
    public static class Char extends Node{
        public final char c;
        public Char(char c){
            this.c = c;
        }
    }

    public static class CharSet extends Node {
        public final Set<Character> chars;
        public CharSet(Set<Character> chars){
            this.chars = chars;
        }
    }

    public static class Concat extends Node{
        public final Node left, right;
        public Concat(Node left, Node right){
            this.left = left;
            this.right = right;
        }
    }

    public static class Alt extends Node{
        public final Node left, right;
        public Alt(Node left, Node right){
            this.left = left;
            this.right = right;
        }
    }

    public static class Star extends Node{
        public final Node lang;
        public Star(Node lang){
            this.lang = lang;
        }
    }

    public static class Iteration extends Node{
        public final Node lang;
        public Iteration (Node lang){
            this.lang = lang;
        }
    }

    public static class Empty extends Node{}

    public static Node ch(char ch) {
        return new Char(ch);
    }

    public static Node str(String s) {
        if (s.isEmpty()) return new Empty();
        Node result = ch(s.charAt(0));
        for (int i = 1; i < s.length(); i++) {
            result = new Concat(result, ch(s.charAt(i)));
        }
        return result;
    }

    public static Node arr(String list) {
        Set<Character> set = new HashSet<>();
        for (int i = 0; i < list.length(); i++) {
            char c = list.charAt(i);
            if (i + 2 <= list.length()-1 && list.charAt(i + 1) == '-') {
                char to = list.charAt(i + 2);
                for (char x = c; x <= to; x++) set.add(x);
                i += 2;
            } else {
                set.add(c);
            }
        }
        return new CharSet(set);
    }

    public static Node concat(Node a, Node b) { return new Concat(a, b); }
    public static Node alt(Node a, Node b)    { return new Alt(a, b); }
    public static Node star(Node a)           { return new Star(a); }
    public static Node plus(Node a)           { return new Iteration(a); }
}
