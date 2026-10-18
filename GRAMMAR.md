# Грамматика языка Funny (LL(1))

Исходная грамматика содержала левую рекурсию, LL(1)-парсер на каждом шаге спускался в левую часть без остановки. Также у операций не было приоритета, из-за чего, например, `+` и `*` были равного приоритетаю
Для устранения этих проблем произвелись следующие преобразования:

1. **Устранение левой рекурсии** для бинарных операторов (по правилу `A -> A a | b` => `A -> b A'`, `A' -> a A' | ε`).
2. **Введение уровней приоритета**: выражения разбиты на уровни, например: вместо `expr -> expr "+" expr` будет `addExpr -> mulExpr "+" mulExpr`
3. **Левая факторизация** для `IDENT` (чтобы отличить вызов функции от доступа к массиву и обычной переменной).
4. **Обработка ассоциативности**: `+`, `-`, `*`, `/` - левоассоциативны (реализованы через циклы `while`), импликация `->` - правоассоциативна (реализована через рекурсию).

### Финальная грамматика

```ebnf
(* Программа и функции *)
program     = { function } ;
function    = IDENT "(" paramList ")"
              [ "requires" predicate ]
              "returns" returnList
              [ "ensures" predicate ]
              [ "uses" localVarList ]
              statement ;
paramList   = [ param { "," param } ] ;
param       = IDENT ":" type ;
returnList  = param { "," param } ;
localVarList= localVar { "," localVar } ;
localVar    = IDENT [ ":" type ] ;
type        = IDENT [ "[" "]" ] ; ("int" или "int[]")

(* Операторы *)
statement   = block | ifStmt | whileStmt | assertStmt | assumeStmt | assign ;
block       = "{" { statement } "}" ;
ifStmt      = "if" "(" condition ")" statement [ "else" statement ] ;
whileStmt   = "while" "(" condition ")" [ "invariant" predicate ] statement ;
assertStmt  = "assert" predicate ";" ;
assumeStmt  = "assume" predicate ";" ;
assign      = IDENT [ "[" expr "]" ] "=" expr ";" ;

(* Выражения *)
expr        = addExpr ;
addExpr     = mulExpr { ("+" | "-") mulExpr } ;
mulExpr     = atom { ("*" | "/") atom } ;
atom        = INT
            | IDENT [ "(" args ")" | "[" expr "]" ]
            | "-" atom
            | "(" expr ")" ;
args        = [ expr { "," expr } ] ;

(* Условия *)
condition   = impl ;
impl        = orExpr [ "->" impl ] ;
orExpr      = andExpr { "or" andExpr } ;
andExpr     = notExpr { "and" notExpr } ;
notExpr     = "not" notExpr | primary ;
primary     = "true" | "false" | comparison | "(" condition ")" ;
comparison  = expr [ ("==" | "!=" | "<" | ">" | "<=" | ">=") expr ] ;

(* Предикаты *)
predicate   = predImpl ;
predImpl    = predOr [ "->" predImpl ] ;
predOr      = predAnd { "or" predAnd } ;
predAnd     = predNot { "and" predNot } ;
predNot     = "not" predNot | predPrimary ;
predPrimary = quantifier | formulaRef
            | "true" | "false" | comparison | "(" predicate ")" ;
quantifier  = ("forall" | "exists") "(" param "|" predicate ")" ;
formulaRef  = IDENT "(" [ expr { "," expr } ] ")" ;