package regex_dfa;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AutomataTest {

    // ==========================================================
    // 1. ТЕСТЫ ДЛЯ AST РЕГУЛЯРОК (Node)
    // ==========================================================
    @Nested
    class NodeTests {
        @Test
        void testNodeChar() {
            Node n = Node.ch('a');
            assertInstanceOf(Node.Char.class, n);
            assertEquals('a', ((Node.Char) n).c);
        }

        @Test
        void testNodeStr() {
            Node n = Node.str("ab");
            assertInstanceOf(Node.Concat.class, n);
        }

        @Test
        void testNodeStrSingle() {
            Node n = Node.str("a");
            assertInstanceOf(Node.Char.class, n);
        }

        @Test
        void testNodeStrEmpty() {
            Node n = Node.str("");
            assertInstanceOf(Node.Empty.class, n);
        }

        @Test
        void testNodeArrRange() {
            Node n = Node.arr("a-c");
            assertInstanceOf(Node.CharSet.class, n);
            Set<Character> chars = ((Node.CharSet) n).chars;
            assertEquals(3, chars.size());
            assertTrue(chars.contains('a'));
            assertTrue(chars.contains('b'));
            assertTrue(chars.contains('c'));
            assertFalse(chars.contains('d'));
        }

        @Test
        void testNodeArrList() {
            Node n = Node.arr("abc");
            assertInstanceOf(Node.CharSet.class, n);
            assertEquals(3, ((Node.CharSet) n).chars.size());
        }

        @Test
        void testNodeConcat() {
            Node n = Node.concat(Node.ch('a'), Node.ch('b'));
            assertInstanceOf(Node.Concat.class, n);
        }

        @Test
        void testNodeAlt() {
            Node n = Node.alt(Node.ch('a'), Node.ch('b'));
            assertInstanceOf(Node.Alt.class, n);
        }

        @Test
        void testNodeStar() {
            Node n = Node.star(Node.ch('a'));
            assertInstanceOf(Node.Star.class, n);
        }

        @Test
        void testNodePlus() {
            Node n = Node.plus(Node.ch('a'));
            assertInstanceOf(Node.Iteration.class, n);
        }
    }

    // ==========================================================
    // 2. ТЕСТЫ ДЛЯ ПОСТРОЕНИЯ НКА (ThompsonBuilder)
    // ==========================================================
    @Nested
    class ThompsonTests {
        @Test
        void testThompsonChar() {
            NFA nfa = new ThompsonBuilder().buildNFA(Node.ch('a'));
            assertEquals(2, nfa.allStates.size());
            assertEquals(1, nfa.acceptTokenId.size());
            assertFalse(nfa.transitions.getOrDefault(nfa.start, Map.of()).getOrDefault('a', Set.of()).isEmpty());
        }

        @Test
        void testThompsonCharSet() {
            NFA nfa = new ThompsonBuilder().buildNFA(Node.arr("a-c"));
            assertEquals(2, nfa.allStates.size());
            Map<Character, Set<Integer>> trans = nfa.transitions.get(nfa.start);
            assertEquals(3, trans.size());
            assertTrue(trans.containsKey('a') && trans.containsKey('b') && trans.containsKey('c'));
        }

        @Test
        void testThompsonEmpty() {
            NFA nfa = new ThompsonBuilder().buildNFA(new Node.Empty());
            assertEquals(2, nfa.allStates.size());
            assertFalse(nfa.epsTransitions.isEmpty());
        }

        @Test
        void testThompsonConcat() {
            NFA nfa = new ThompsonBuilder().buildNFA(Node.concat(Node.ch('a'), Node.ch('b')));
            assertEquals(4, nfa.allStates.size());
            assertEquals(1, nfa.acceptTokenId.size());
            assertFalse(nfa.epsTransitions.isEmpty());
        }

        @Test
        void testThompsonAlt() {
            NFA nfa = new ThompsonBuilder().buildNFA(Node.alt(Node.ch('a'), Node.ch('b')));
            assertEquals(6, nfa.allStates.size());
            assertEquals(1, nfa.acceptTokenId.size());
            assertFalse(nfa.epsTransitions.isEmpty());
        }

        @Test
        void testThompsonStar() {
            NFA nfa = new ThompsonBuilder().buildNFA(Node.star(Node.ch('a')));
            assertEquals(4, nfa.allStates.size());
            assertEquals(1, nfa.acceptTokenId.size());
            // Проверяем, что есть петля (ε-переходы)
            assertTrue(nfa.epsTransitions.size() >= 2);
        }

        @Test
        void testThompsonPlus() {
            NFA nfa = new ThompsonBuilder().buildNFA(Node.plus(Node.ch('a')));
            // A+ = A A* => 2 + 4 = 6 состояний (по Томпсону)
            assertEquals(6, nfa.allStates.size());
        }

        @Test
        void testThompsonUnion() {
            Token a = new Token("A", Node.ch('a'), false);
            Token b = new Token("B", Node.ch('b'), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a, b));
            assertEquals(5, nfa.allStates.size()); // 1 старт + 2 + 2
            assertEquals(2, nfa.acceptTokenId.size());
            assertEquals(Set.of(0, 1), new HashSet<>(nfa.acceptTokenId.values()));
        }
    }

    // ==========================================================
    // 3. ТЕСТЫ ДЛЯ ДЕТЕРМИНИЗАЦИИ (SubsetConstruction)
    // ==========================================================
    @Nested
    class SubsetTests {
        @Test
        void testSubsetChar() {
            NFA nfa = new ThompsonBuilder().buildNFA(Node.ch('a'));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            assertEquals(2, dfa.size());
        }

        @Test
        void testSubsetStar() {
            Token a = new Token("A", Node.star(Node.ch('a')), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            assertEquals(2, dfa.size()); // Старт (принимает "") и после 'a' (принимает)
            assertEquals(2, dfa.acceptTokenId.size());
        }

        @Test
        void testSubsetAlt() {
            Token a = new Token("A", Node.alt(Node.ch('a'), Node.ch('b')), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            assertEquals(3, dfa.size()); // Старт, после 'a', после 'b'
        }

        @Test
        void testSubsetConcat() {
            Token a = new Token("A", Node.concat(Node.ch('a'), Node.ch('b')), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            assertEquals(3, dfa.size()); // "", "a", "ab", ловушка не считается (ее еще нет)
        }
    }

    // ==========================================================
    // 4. ТЕСТЫ ДЛЯ МИНИМИЗАЦИИ (Minimize)
    // ==========================================================
    @Nested
    class MinimizeTests {
        @Test
        void testMinimizeStar() {
            Token a = new Token("A", Node.star(Node.ch('a')), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            DFA min = new Minimize().minimize(dfa);
            assertEquals(1, min.size(), "ДКА для a* должен склеиться в 1 состояние");
        }

        @Test
        void testMinimizeAlt() {
            Token a = new Token("A", Node.alt(Node.ch('a'), Node.ch('b')), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            DFA min = new Minimize().minimize(dfa);
            assertEquals(2, min.size(), "ДКА для a|b должен склеиться в 2 состояния");
        }

        @Test
        void testMinimizeNoChange() {
            Token a = new Token("A", Node.ch('a'), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            DFA min = new Minimize().minimize(dfa);
            assertEquals(dfa.size(), min.size(), "Минимизация a не должна менять число состояний");
        }
    }

    // ==========================================================
    // 5. ТЕСТЫ ДЛЯ ЛОВУШКИ (TrapAdder)
    // ==========================================================
    @Nested
    class TrapTests {
        @Test
        void testTrapAdded() {
            Token a = new Token("A", Node.ch('a'), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            int before = dfa.size();

            new TrapAdder().addTrap(dfa);
            assertEquals(before + 1, dfa.size());
            assertTrue(dfa.trap >= 0);
        }

        @Test
        void testTrapCompleteness() {
            Token a = new Token("A", Node.ch('a'), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            new TrapAdder().addTrap(dfa);

            // Проверяем, что из каждого состояния есть переход по всем ASCII (0..127)
            for (int s : dfa.allStates) {
                Map<Character, Integer> row = dfa.transitions.getOrDefault(s, Map.of());
                for (char c = 0; c < 128; c++) {
                    assertTrue(row.containsKey(c), "У состояния " + s + " нет перехода по символу " + (int)c);
                }
            }
        }

        @Test
        void testTrapIsNotAccepting() {
            Token a = new Token("A", Node.ch('a'), false);
            NFA nfa = new ThompsonBuilder().union(List.of(a));
            DFA dfa = new SubsetConstruction().buildDFA(nfa);
            new TrapAdder().addTrap(dfa);

            assertFalse(dfa.acceptTokenId.containsKey(dfa.trap), "Ловушка не должна быть принимающим состоянием");
        }
    }
}