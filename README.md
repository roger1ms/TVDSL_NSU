# Проект TVDSL: Лексер и Парсер языка Funny

Проект реализует два этапа компилятора для языка Funny:
1. **Генерация лексера (HW1)**: построение минимального ДКА из регулярных выражений.
2. **Синтаксический анализ (HW2)**: LL(1) парсер со сборкой AST и обработкой ошибок.

## Структура проекта
- `src/main/java/regex_dfa/` - Генератор автомата.
- `src/main/java/lexer/` - Лексер. Загружает CSV, сгенерированные в regex_dfa, и разбивает текст на токены.
- `src/main/java/parser/` - Парсер LL(1), классы AST.
- `src/test/java/` — Тесты.
- `export/` - Таблицы переходов ДКА.
- `GRAMMAR.md` - Финальная LL(1) грамматика языка Funny.
- `FUNNY.md` — Описание языка.
- `REPORT.md` - Отчёт по генерации ДКА.

## Требования
- **Java 21+**
- **Maven**

## Как запустить

Все команды выполняются из корня проекта.

### 1. Запуск тестов
Для запуска:
```bash
mvn test
```

### 2. Генерация ДКА 
При добавлении новых состояний нужно перегенерировать автомат.
Для запуска из консоли:
```bash
mvn compile exec:java -Dexec.mainClass="regex_dfa.Main"
```
Для запуска из IDE:
1. Откройте `src/main/java/regex_dfa/Main.java` 
2. Запустите программу

### 3. Запуск парсера
Из консоли:
```bash
mvn compile exec:java -Dexec.mainClass="parser.Main" -Dexec.args="input.funny"
```
(`input.funny` - путь к файлу с кодом на языке Funny, в корне проекта, пример: `f(a: int[], n: int) requires n > 0 returns r: int ensures forall (i: int | i >= 0 and i < n -> a[i] > 0) { r = 0; }`)

Для запуска из IDE:
1. Откройте `src/main/java/parser/Main.java`
2. В конфигурации к запуску укажите в **Program arguments** `input.funny` с вашим кодом, убедитесь, что файл лежит в корне
3. Запустите программу