<!-- TOC -->
* [Battery Script Introduction](#battery-script-introduction)
  * [Design Goals](#design-goals)
  * [Grammar and Lexer Rules](#grammar-and-lexer-rules)
* [Language Guide](#language-guide)
  * [Comments](#comments)
  * [Variables & Scoping](#variables--scoping)
    * [Transient / Scoped Variables](#transient--scoped-variables)
  * [Operators & Expressions](#operators--expressions)
  * [Data Types & Literals](#data-types--literals)
    * [Numeric Literals](#numeric-literals)
    * [String Literals & Text Blocks](#string-literals--text-blocks)
    * [Date and Time Literals](#date-and-time-literals)
    * [Collections (Arrays, Lists, Sets, Maps)](#collections-arrays-lists-sets-maps)
      * [Array & Collection Operations](#array--collection-operations)
  * [Control Flow](#control-flow)
    * [Conditional Statements (`if / else if / else`)](#conditional-statements-if--else-if--else)
    * [Range Loop (`for ... from ... to`)](#range-loop-for--from--to)
    * [Iteration Loop (`foreach`)](#iteration-loop-foreach)
    * [Conditional Loop (`while`)](#conditional-loop-while)
  * [Fork / Join Concurrency](#fork--join-concurrency)
  * [Java Interoperability & Reflection](#java-interoperability--reflection)
* [Standard Function Library](#standard-function-library)
* [Interactive Testing via Shell](#interactive-testing-via-shell)
<!-- TOC -->

# Battery Script Introduction

Battery script is designed to provide dynamics and variance for simulating actual 
production-like SQL workloads. While the SQL language is declarative and set-based, 
Battery script is imperative and procedural, allowing for common programming 
language constructs and flow control. 

## Design Goals

The primary design goal of battery script is _simplicity_ while providing the ability 
to do complex tasks. This is achieved through a basic, procedural language design with 
variables, dynamic types, common expressions, statements and commands using a fairly 
narrow function library for SQL execution.

Why not use JavaScript, TypeScript or something similar you ask? While established 
programming/scripting languages have their merits, there is also a higher barrier of 
entry for beginners. In addition, these languages are packed with advanced features 
and idioms that are not particularly useful in this context.

## Grammar and Lexer Rules

The parser generator [ANTLR4](https://www.antlr.org/) grammar and lexer rules for battery script are available here:

- [Parser Grammar](src/main/java/io/battery/script/BatteryParser.g4)
- [Lexer Rules](src/main/java/io/battery/script/BatteryLexer.g4)

# Language Guide

## Comments

Battery script supports single-line and multi-line comments:

```java
// Single-line comment style 1
-- Single-line comment style 2 (SQL style)
# Single-line comment style 3 (Shell style)

/*
   Multi-line block comment
*/
```

## Variables & Scoping

Variables serve as placeholders for values of any supported data type. All variables are dynamically typed and declared via assignment:

```java
A = 1;
B = 2.5;
C = "The quick brown fox jumps over the lazy dog";
```

The data type is inferred at the time of assignment and updates dynamically when a new value of a different type is assigned.

### Transient / Scoped Variables

Variables whose names start with an underscore (such as `_temp`, `_index`, or `_x`) are treated as **transient / local** variables. They are scoped locally and omitted from the returned script state and interactive shell state capture.

```java
_secret = "temporary"; // Will not be retained in returned script state
visible = 123;         // Retained in script state
```

## Operators & Expressions

Operators perform computations on operands. Battery script follows standard operator precedence:

- **Arithmetic Operators**:
  - Plus (`+`)
  - Minus (`-`)
  - Multiply (`*`)
  - Divide (`/`)
  - Power of (`^`)
  - Modulus (`%`)
- **Logical / Conditional Operators**:
  - Logical conjunction (`and`, `&&`)
  - Logical disjunction (`or`, `||`)
  - Logical negation (`not`, `!`)
- **Comparison Operators**:
  - Equal (`==`)
  - Not equal (`!=`)
  - Less than (`<`)
  - Less than or equal (`<=`)
  - Greater than (`>`)
  - Greater than or equal (`>=`)
- **Assignment Operator**:
  - Assignment (`=`)
- **Array / Collection Operators**:
  - Membership (`in`)
  - Element selection by 0-based index (`select`)

Example:

```java
A = 1;
B = -2.5;
C = A + B; // -1.5
if (C < 0) {
    log.info("C is negative: %s", [C]);
}
```

## Data Types & Literals

Battery script natively supports the following types and literal syntaxes:

### Numeric Literals

- **Integer / Long**: `42`, `1000L`
- **Double**: `3.14`, `12.5d`
- **BigDecimal**: `12.50bd`, `100bd` (useful for high-precision financial/money arithmetic)

### String Literals & Text Blocks

Single-line strings can be enclosed in double or single quotes, such as `"abc"` or `'abc'`. They support Java style
escape sequences, octal and unicode escapes. A single-quoted string can't contain an unescaped `'`, so 
use `"it's"` or `'it\'s'`.

Multiline strings (text blocks) use triple quotes `"""`, ideal for embedding multiline SQL queries. 
Text blocks are kept as written, so backslashes are not treated as escapes:

```java
query = """
    SELECT id, name, balance
    FROM account
    WHERE balance > ?
    ORDER BY id
""";
```

### Date and Time Literals

Dedicated literal syntaxes allow creating Java `time` types directly:

- **LocalDate**: `{d'2024-12-24'}`
- **LocalTime**: `{t'23:59:59'}`
- **LocalDateTime**: `{dt'2024-12-24 23:59:59'}`

### Collections (Arrays, Lists, Sets, Maps)

Battery script provides literal constructors for Java collections:

- **Array**: `[1, 2, 3]`
- **List collection (`java.util.List`)**: `L[1, 2, 3]`
- **Set collection (`java.util.Set`)**: `S[1, 2, 3]`
- **Map collection (`java.util.Map`)**: `M['key1', 'val1', 'key2', 'val2']`

#### Array & Collection Operations

```java
items = ["Craig", 32, true];

// Membership check with `in`
foundIt = 32 in items; // true

// Zero-based element indexing with `select`
name = items select 0; // 'Craig'
```

## Control Flow

### Conditional Statements (`if / else if / else`)

```java
limit = 10;
rows = jdbc.queryForList("select * from account limit " + limit);

if (!rows.isEmpty()) {
    log.info("Found %d accounts", [rows.size()]);
} else if (limit == 0) {
    log.warn("Limit was zero");
} else {
    log.info("No rows found!");
}
```

### Range Loop (`for ... from ... to`)

Executes a block across an integer range (with optional step):

```java
for i from 1 to 5 {
    log.info("Step: %d", [i]);
}

// With explicit step
for i from 10 to 0 step -2 {
    log.info("Countdown: %d", [i]);
}
```

### Iteration Loop (`foreach`)

Iterates over arrays and collections. Inside the loop body, `_x` represents the current element, 
and `_forEachIndex` provides the 0-based index:

```java
names = ["Alice", "Bob", "Charlie"];
foreach (names) {
    log.info("User #%d is %s", [_forEachIndex, _x]);
}
```

### Conditional Loop (`while`)

Repeats execution while a condition evaluates to `true`:

```java
count = 3;
while (count > 0) {
    log.info("Remaining: %d", [count]);
    count = count - 1;
}
```

## Fork / Join Concurrency

Battery script supports lightweight asynchronous task execution using `fork` and `join`:

```java
f1 = fork {
    id = jdbc.update("insert into account (id,name,balance) values(unordered_unique_id(),?,?) returning id",
            [gen.randomWord(12), gen.randomDouble(10.00, 100.00)]);
    return id;
};

f2 = fork {
    id = jdbc.update("insert into account (id,name,balance) values(unordered_unique_id(),?,?) returning id",
            [gen.randomWord(12), gen.randomDouble(10.00, 100.00)]);
    return id;
};

f3 = fork {
    id = jdbc.update("insert into account (id,name,balance) values(unordered_unique_id(),?,?) returning id",
            [gen.randomWord(12), gen.randomDouble(10.00, 100.00)]);
    return id;
};

// Wait for all forked tasks to complete
join [f1, f2, f3];

ids = [f1.get(), f2.get(), f3.get()];
log.info("Created accounts: %s", [ids]);
```

## Java Interoperability & Reflection

Battery script allows seamless access to Java classes, static methods, static fields, and instance methods:

- **Static Method Invocations**:
  ```java
  now = java.time.LocalDate.now();
  maxVal = java.lang.Math.max(10, 20);
  ```
- **Static Field Access**:
  ```java
  pi = java.lang.Math.PI;
  ```
- **Instance Methods**:
  ```java
  greeting = "hello world";
  upper = greeting.toUpperCase();
  len = greeting.length();
  ```

# Standard Function Library

Built-in functions are grouped into standard namespaces:

| Namespace  | Description                                    |
|------------|------------------------------------------------|
| `jdbc`     | SQL queries, updates, and key retrieval        |
| `gen`      | Random data generators for workload simulation |
| `std`      | Core utilities, math, and time                 |
| `log`      | Formatted logging to application logs          |
| `network`  | Network and IP address generators              |
| `wgs`      | Coordinates, positions, and geodetic math      |
| `encoding` | Cryptographic hashing and encoding             |

# Interactive Testing via Shell

Battery script expressions and statements can be executed interactively in the Spring Shell CLI:

* `execute` / `e`: Execute an ad-hoc script expression:
  ```bash
  battery:$ execute "x = gen.randomInt(1, 100); log.info('x=%d', [x]);"
  ```
* `execute --capture`: Execute a script and capture the resulting variable state into the interactive session:
  ```bash
  battery:$ execute --capture "id = gen.randomUUID();"
  ```
* `show state` / `ss`: Display all currently captured variables and their values.
* `wipe state` / `ws`: Clear captured session state.
* `functions` / `f`: List all registered script functions, namespaces, and parameter signatures.