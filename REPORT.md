# Отчёт по HW1 (Генерация DFA)

## Список регулярных выражений токенов

| Токен | Регулярка | skip |
|---|---|---|
| FUNCTION | `function` | |
| RETURNS | `returns` | |
| REQUIRES | `requires` | |
| ENSURES | `ensures` | |
| USES | `uses` | |
| WHILE | `while` | |
| IF | `if` | |
| ELSE | `else` | |
| ASSERT | `assert` | |
| ASSUME | `assume` | |
| INVARIANT | `invariant` | |
| FORALL | `forall` | |
| EXISTS | `exists` | |
| TRUE | `true` | |
| FALSE | `false` | |
| AND | `and` | |
| OR | `or` | |
| PIPE | `\|` | |
| NOT | `not` | |
| LENGTH | `length` | |
| IDENT | `[a-zA-Z_][a-zA-Z0-9_]*` | |
| INT | `0 \| [1-9][0-9]*` | |
| WS | `[ \t\r\n]+` | ✓ |
| COMMENT | `//[^\n]*` | ✓ |
| EQ | `==` | |
| NEQ | `!=` | |
| LE | `<=` | |
| GE | `>=` | |
| IMPL | `->` | |
| FATARROW | `=>` | |
| LPAREN | `(` | |
| RPAREN | `)` | |
| LBRACE | `{` | |
| RBRACE | `}` | |
| LBRACKET | `[` | |
| RBRACKET | `]` | |
| COMMA | `,` | |
| SEMI | `;` | |
| COLON | `:` | |
| PLUS | `+` | |
| MINUS | `-` | |
| STAR | `*` | |
| SLASH | `/` | |
| LT | `<` | |
| GT | `>` | |
| ASSIGN | `=` | |

## Размеры автоматов

| Этап | Состояний |
|---|-----------|
| НКА  | 291 |
| ДКА до минимизации | 124 |
| ДКА после минимизации | 120 |
| ДКА с ловушкой | 121 |

## Ловушка

Состояние `120` не принимающее. Из каждого состояния ДКА по каждому ASCII-символу `0..127` есть переход: либо настоящий, либо в ловушку. Из ловушки все переходы - в неё же.

## Приоритет токенов

Ключевые слова объявлены раньше IDENT - их `id` меньше. При конфликте (например, `"if"` подходит и под `IF`, и под `IDENT`) побеждает минимальный id - `IF`.