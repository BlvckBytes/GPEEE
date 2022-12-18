# GPEEE

![build](https://github.com/BlvckBytes/GPEEE/actions/workflows/build.yml/badge.svg)
[![coverage](https://codecov.io/gh/BlvckBytes/GPEEE/branch/main/graph/badge.svg?token=WGWX8IDT00)](https://codecov.io/gh/BlvckBytes/GPEEE)

![logo](readme_images/logo.png)

The opensource `General Purpose Environment Expression Evaluator` which you most definitely
want to integrate into your next project. It's pronounced "cheapy", matching it's slim technical
cost when comparing against a project without `GPEEE`.

WARNING: This readme is out of sync... I need to get to writing a properly formatted and in-sync version soon.

## Table of Contents
- [Current State](#current-state)
- [Mission Statement](#mission-statement)
- [How It Works](#how-it-works)
  - [Evaluator API](#evaluator-api)
  - [The Evaluation Environment](#the-evaluation-environment)
  - [Expression Values](#expression-values)
  - [Resolving Variables](#resolving-variables)
  - [Calling Functions](#calling-functions)
  - [Callback Functions](#callback-functions)
  - [Terminal Values](#terminal-values)
- [Grammar](#grammar)
  - [Numbers](#numbers)
  - [Strings](#strings)
  - [Literals](#literals)
  - [Identifiers](#identifiers)
  - [Operators And Precedences](#operators-and-precedences)
  - [Parentheses](#parentheses)
  - [Function Invocation](#function-invocation)
  - [Callback Function](#callback-function)
  - [If Then Else](#if-then-else)
  - [Indexing](#indexing)
  - [Member Access](#member-access)
  - [Complete Definition](#complete-definition)

## Current State

The project is still far from being production-ready, but here you can have a look at the most impressive thing it
can accomplish so far:

<details>
<summary>Click Me</summary>

Expression:

```
"my prefix: " & iter_cat(my_items, (it, ind) -> "(" & ind & " -> " & it & ")", "|", "no items available")
```

Result:

```
my prefix: (0 -> 1)|(1 -> 3)|(2 -> 5)|(3 -> 21)|(4 -> 49)
```

On argument type mismatches:

```
5: "my prefix: " & iter_cat(4, (it, ind) -> "(" & ind & " -> " & it & ")", "|", "no items available")
                            ^ Invalid function argument, expected value of type java.util.Collection but got java.lang.Long
```

Which uses the standard `iter_cat` function:

```java
/**
 * Iteration concatenation - iter_cat
 *
 * Concatenates a collection of items by running each through a callback expression
 * which will format it to a string value and then joins them all with the provided
 * separator. If there are no items, the fallback will be returned only.
 */
public class IterCatFunction extends AStandardFunction {

  @Override
  public Object apply(IEvaluationEnvironment env, List<@Nullable Object> args) {
    // Retrieve arguments
    Collection<?> items = (Collection<?>) Objects.requireNonNull(args.get(0));
    AExpressionFunction mapper = (AExpressionFunction) Objects.requireNonNull(args.get(1));
    String separator = env.getValueInterpreter().asString(args.get(2));
    @Nullable String fallback = env.getValueInterpreter().asString(args.get(3));

    StringBuilder result = new StringBuilder();

    // Loop all items with their indices
    int c = 0;
    for (Object item : items) {
      result.append(result.length() == 0 ? "" : separator).append(
        mapper.apply(env, List.of(item, c++))
      );
    }

    // No items available but a fallback string has been supplied
    if (items.size() == 0 && fallback != null)
      return fallback;

    // Respond with the built-up result
    return result.toString();
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    // iter_cat(items, (it, ind) -> (..), "separator", "no items fallback")
    return List.of(
      new ExpressionFunctionArgument("items", true, Collection.class),
      new ExpressionFunctionArgument("mapper", true, AExpressionFunction.class),
      new ExpressionFunctionArgument("separator", true, String.class),
      new ExpressionFunctionArgument("fallback", false, String.class)
    );
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("iter_cat", this);
  }
}
```
</details>

## Mission Statement

While this *is* a general purpose evaluator, it has been designed to solve a very specific problem
many bukkit plugin developers face: Allowing the user to easily describe dynamic and possibly complex
behavior inside a *YAML* configuration file. Many successful projects offer such a feature, but - at least
to my knowledge - they all painstakingly implement their own evaluator. Not only does this suck for
the end user who has to learn a million different flavors for basic operation expressions, but it also
heavily constrains lots of plugin developers who don't know how to write a parser in the user experience
of their software.

In order to keep this already pretty complex project as dead simple as possible, I've set a few
main guidelines in stone:

* Very clean, maintainable and well documented codebase to make it accessible for everyone
* Implement all generally known operators but **don't** add anything fancy
* Expressions will **not** keep any state (no variable declarations, etc)
* Next to basic terminal values and operators there will be **no** keywords
* **All** control flow and complex logic will be outsourced to Java and hidden behind functions

Without adhering to these, I'm sure the project would grow into an unmaintainable beast of "one more
feature please". The restrictions may seem harsh, but you're still able to get up and running with
all customizations by defining your own *evaluation environment* exactly to your needs in no time.

## How It Works

The working principle of this evaluator is as simple as it gets: You provide an *evaluation environment*
as well as a string containing the target expression and the evaluator returns an *evaluation result* or
throws an error to be properly handled by the caller.

### Evaluator API

```java
public interface IExpressionEvaluator {

  /**
   * Parses an input string into an abstract syntax tree (AST) to be evaluated
   * later on and possibly be reused for multiple evaluations with multiple environments.
   * @param input Input to parse
   * @return Root node of the AST
   * @throws AEvaluatorError Error during the parsing process
   */
  AExpression parseString(String input) throws AEvaluatorError;

  /**
   * Evaluates a previously parsed expression within a provided evaluation environment.
   * @param expression Expression to evaluate
   * @return Resulting expression value
   * @throws AEvaluatorError Error during the interpretation process
   */
  Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError;

}
```

This interface is implemented by the `GPEEE` class, which can be quickly instantiated with an optional logger and a path to the function folder:

```java
public GPEEE(@Nullable ILogger logger, @Nullable String functionFolder);
```

The `logger` will get called with many internal debug messages which can help you to understand how the inner workings
of this state machine operate. It also logs errors when loading functions.

### The Evaluation Environment

An *evaluation environment* consists of the following properties:

```java
public interface IEvaluationEnvironment {

  /**
   * Mapping identifiers to available functions which an expression may invoke
   */
  Map<String, FExpressionFunction> getFunctions();

  /**
   * Mapping identifiers to available live variables which an expression may resolve
   */
  Map<String, Supplier<Object>> getLiveVariables();

  /**
   * Mapping identifiers to available static variables which an expression may resolve
   */
  Map<String, Object> getStaticVariables();

  /**
   * Get the value interpreter used to interpret values when doing any kind of
   * operation on them which they'd usually not support naturally.
   *
   * Most of the time, you'd want to provide {@link me.blvckbytes.gpeee.GPEEE#STD_VALUE_INTERPRETER}
   */
  IValueInterpreter getValueInterpreter();

}
```

An `identifier` is basically a unique name which can be used inside of expressions and underlies these
[constraints](#identifiers). The only difference between live variables and static variables is that a
live variable will be resolved by calling the supplier function while a static variable has to be set
in stone ahead of time.

An *expression function* can be easily created by adhering to the following signature:

```java
@FunctionalInterface
public interface FExpressionFunction {

  /**
   * Called whenever a function call to the registered corresponding
   * identifier is performed within an expression
   * @param environment A reference to the current environment
   * @param args Arguments supplied by the invocation
   * @return Return value of this function
   */
  Object apply(IEvaluationEnvironment environment, List<Object> args);

}
```

### Expression Values

Expressions interface with the world around them using the Java Object. All operations can be - by design - performed on
all types, as the Interpreter makes use of a so-called `IValueInterpreter`, provided by the `IEvaluationEnvironment`.
This object decides which value corresponds to an object in different situations.

The following tables are an overview of standard value interpretation:

**Math operations:**

| LHS    | Operator    | RHS    | Result Type | Notes                     |
|--------|-------------|--------|-------------|---------------------------|
| Long   | +,-,*,^,%   | Long   | Long        | /                         |
| Long   | /           | Long   | Long/Double | Double only on remainders |
| Double | +,-,*,/,^,% | Long   | Double      | /                         |
| Double | +,-,*,/,^,% | Double | Double      | /                         |

Since equality- as well as comparison operations take place mostly on numbers only, all other values have number
fallback values assigned to them:

| Type            | Fallback | Notes              |
|-----------------|----------|--------------------|
| STRING (empty)  | 0        | An empty string    |
| STRING          | 1        | A non-empty string |
| BOOLEAN (true)  | 1        | /                  |
| BOOLEAN (false) | 0        | /                  |
| NULL            | 0        | /                  |
| FUNCTION        | 1        | /                  |
| LIST (empty)    | 0        | An empty list      |
| LIST            | 1        | A non-empty list   |

**Equality operations:**

The non-exact operators (`==`, `!=`) match, if the content of two values is/isn't the same. This also means that
a string containing a number's sequence of characters will equal (`==`) to that number. If the type is also to be
checked, the use of exact operators (`===`, `!==`) is required.

Lists are equal if either their reference is equal or all of their elements are equal (strict or not depends on
the used operator on the lists).

For Strings, non-exact means ignore casing as well as leading/trailing spaces when comparing while exact checks these properties.

**Comparison operations:**

Comparison can only either take place on numbers or on values implementing the `Comparable` interface which are of same type.
In all other instances, both sides of the operator will be interpreted as longs.

**Boolean operations:**

Null values are considered to equal to `false` while all other values only equal true if they compare positively against
zero (> 0). For non-numeric values this means that the fallback of interpreting them as a long will be made use of.

**Concatenation operation:**

Operands of a concatenation operation will always be interpreted as a string and just added together.

### Resolving Variables

A variable is resolved as soon as you write out an `identifier` that's available in the current environment.

### Calling Functions

You should have a look at the exact [grammar](#function-invocation) of a function invocation. Summary: Provide an identifier,
open a parenthesis, specify 0-n parameters (which can be expressions too) and close the parenthesis again. Something like this:
`myFunction("hello world", 1, .3, null, true, false)`.

### Callback Functions

You should have a look at the exact [grammar](#callback-function) of a callback function. Summary: Open a parenthesis,
specify 0-n parameters, close the parenthesis again, write an arrow operator and follow that up by an expression. Something like this:
`foreach(items, (index) -> "index=" & index, "\n")`.

### Terminal Values

Terminal values are immediate values which do not resolve any further and are set in stone from the
point of writing them within the expression onwards.

| Type    | Example                                    |
|---------|--------------------------------------------|
| Long    | 1, -1                                      |
| Double  | .1, -.1, 3.1415                            |
| String  | "Escaped \\" double- and \s single quote " |
| Boolean | true, false                                |
| Null    | null                                       |

## Grammar

All railroad diagrams have been created using [rr](https://bottlecaps.de/rr/ui).

### Numbers

There are two types of numbers, floating point as well as whole numbers.

We start out by defining what a single digit has to look like:

```ebnf
Digit ::= [0-9]
```

![digit](readme_images/railroad_digit.png)

Multiple digits in combination with an optional, leading minus sign compose a long.

```ebnf
Long ::= "-"? Digit+
```
![long](readme_images/railroad_long.png)

If the number contains a decimal, it's a double. It may omit the zero right before the dot.

```ebnf
Double ::= "-"? Digit* "." Digit+
```
![double](readme_images/railroad_double.png)

### Strings

A string is always surrounded by two double-quotes `"` and may only contain characters other than
double-quotes. To add a double-quote character without closing the string, it has to be escaped `\"`.
Since YAML strings can be cumbersome when it comes to single-quotes, `\s` will be replaced by a single-quote `'`.

```ebnf
String ::= '"' ('\"' | [^"] | "\s")* '"'
```

![string](readme_images/railroad_string.png)


### Literals

A literal is a fixed value represented by a human-readable word. These words are reserved and cannot be used as identifiers.

```ebnf
Literal ::= "true" | "false" | "null"
```

![literal](readme_images/railroad_literal.png)

### Identifiers

An identifier is either a function name, a variable name or a lookup table name. These names are provided by the
evaluation context, may only start with letters and can only consist of letters, digits and underscores.

```ebnf
Letter ::= [A-Za-z]
Identifier ::= Letter (Digit | Letter | '_')*
```
![letter](readme_images/railroad_letter.png)

![identifier](readme_images/railroad_identifier.png)

### Operators And Precedences

In order to not have to call functions for simple operations and to improve on readability,
some of the most used operators have been implemented. Operators are listed top to bottom, in descending precedence.

| Operator                    | Example                      | Description                                          | Precedence |
|-----------------------------|------------------------------|------------------------------------------------------|------------|
| (...)                       | (5 + 3)                      | Parentheses have the highest possible precedence     | 0          |
| **Member Access**           |
| .                           | A.B                          | Accesses the member called B of object A             | 1          |
| **Indexing**                |
| []                          | my_map["my_key"], my_list[0] | Access a map key or a list index                     | 2          |
| **Negation Operator**       |
| not                         | not A                        | Yields the inverse of A                              | 3          |
| **Exponentiation Operator** |
| ^                           | A ^ B                        | Yields A to the power of B                           | 4          |
| **Multiplicative Operator** |
| *                           | A * B                        | Yields the product of A and B                        | 5          |
| /                           | A / B                        | Yields the quotient of A and B                       | 5          |
| %                           | A % B                        | Yields the remainder of dividing A by B              | 5          |
| **Additive Operator**       |
| +                           | A + B                        | Yields the sum of A and B                            | 6          |
| -                           | A - B                        | Yields the difference of A and B                     | 6          |
| **Comparison Operator**     |
| \>                          | A \> B                       | Yields `true` if A is greater than B                 | 7          |
| <                           | A < B                        | Yields `true` if A is less than B                    | 7          |
| \>=                         | A \>= B                      | Yields `true` if A is greater than or equal to B     | 7          |
| <=                          | A <= B                       | Yields `true` if A is less than or equal to B        | 7          |
| **Equality Operator**       |
| ==                          | A == B                       | Yields `true` if A and B equal ignoring casing       | 8          |
| ===                         | A === B                      | Yields `true` if A and B equal exactly               | 8          |
| !=                          | A != B                       | Yields `true` if A and B don't equal ignoring casing | 8          |
| !==                         | A !== B                      | Yields `true` if A and B don't equal exactly         | 8          |
| **Conjunction Operator**    |
| and                         | A and B                      | Yields `true` if both A and B yield `true`           | 9          |
| **Disjunction Operator**    |
| or                          | A or B                       | Yields `true` if either A or B yields `true`         | 10         |
| **Concatenation Operator**  |
| &                           | A & B                        | Concatenates the contents of A and B                 | 11         |

### Parentheses

To enforce a desired order of operations, expressions may be wrapped in parentheses. There is no limit for how many
parentheses the input can contain, they just need to be balanced (every opening parenthesis has to be closed later
down the expression again). While `5 + 2 * 5` would produce 15, `(5 + 2) * 5` will yield 35.

The expression within parentheses can be modified by prepending an operator right before it. The following operators
are available as parenthesis modifiers:

| Operator | Example      | Description                                         |
|----------|--------------|-----------------------------------------------------|
| -        | -(1 + 2)     | Flips the sign of the parentheses expression result |
| not      | not (a && b) | Inverts the boolean parentheses expression result   |

![expression](readme_images/railroad_expression.png)

### Function Invocation

Functions are provided by the evaluation context and arbitrarily many may be called an arbitrary amount of times within
the expression. Prepending the invocation with a minus sign will flip the function return value's sign. If multiple
trailing arguments of a function are optional, they can be explicitly targeted using named arguments.

An invocation is defined like this:

```ebnf
FunctionArgument ::= (Identifier "=")? Expression
FunctionInvocationExpression ::= "-"? Identifier "(" (FunctionArgument | (FunctionArgument ("," FunctionArgument)*))? ")"
```

![function argument](readme_images/railroad_function_argument.png)

![function invocation](readme_images/railroad_function_invocation.png)

### Callback Function

A callback function is an expression which can be programmatically evaluated within a Java function which has been invoked by an
expression. This way, a back and forth type data flow becomes possible.

Callbacks may have an arbitrary amount of parameters which then at runtime become available within the expression
noted after the arrow operator.

```ebnf
CallbackExpression ::= "(" (Identifier | (Identifier ("," Identifier)*))? ")" "->" Expression
```

### If Then Else

Basic decisions can be accomplished by making use of the if-then-else expression. The keyword `if` has to be followed
by an expression which will be interpreted as a boolean, followed by the keyword `then`, followed by the expression to
evaluate in the positive case, followed by the keyword `else`, followed by the negative case expression.

```ebnf
IfThenElseExpression ::= "if" Expression "then" Expression "else" Expression
```

![if_then_else](readme_images/railroad_if_then_else.png)

### Indexing

Lists, Maps and Arrays may be indexed by passing an expression as a key between two brackets after the identifier
of the target variable.

```ebnf
IndexExpression ::= Expression "[" Expression "]"
```

![indexing](readme_images/railroad_indexing.png)

### Member Access

If the value at hand is an object with it's own fields, it's member can be accessed using the member access notation. Specify
the identifier of the target object, followed by a dot `.`, followed by it's target field. These access notations can be chained
as often as necessary to reach deep into the object.

The target field's name may also be an expression, allowing for quick dynamic field access.

```ebnf
MemberAccessExpression ::= (Expression | MemberAccessExpression) "." Expression
```

![member_access](readme_images/railroad_member_access.png)

### Complete Definition

The following [EBNF](https://www.w3.org/2001/06/blindfold/grammar) defines the whole grammar which this parser understands:

This EBNF is badly out of sync... don't try to read it.

```ebnf
Digit ::= [0-9]
Letter ::= [A-Za-z]

Long ::= "-"? Digit+
Double ::= "-"? Digit* "." Digit+
String ::= '"' ('\"' | [^"] | "\s")* '"'
Identifier ::= Letter (Digit | Letter | '_')*
Literal ::= "true" | "false" | "null"

AdditiveOperator ::= "+" | "-"
MultiplicativeOperator ::= "*" | "/" | "%"
EqualityOperator ::= "==" | "!=" | "===" | "!=="
ComparisonOperator ::= ">" | "<" | ">=" | "<="

PrimaryExpression ::= Long | Double | String | Identifier | Literal

IfThenElseExpression ::= "if" Expression "then" Expression "else" Expression

FunctionArgument ::= (Identifier "=")? Expression
FunctionInvocationExpression ::= "-"? Identifier "(" (FunctionArgument | (FunctionArgument ("," FunctionArgument)*))? ")"

CallbackExpression ::= "(" (Identifier | (Identifier ("," Identifier)*))? ")" "->" Expression

MemberAccessExpression ::= Expression "." Expression

IndexExpression ::= Expression "[" Expression "]"

NegationExpression ::= "not"? Expression
ExponentiationExpression ::= NegationExpression ("^" Expression)*
MultiplicativeExpression ::= ExponentiationExpression (MultiplicativeOperator Expression)*
AdditiveExpression ::= MultiplicativeExpression (AdditiveOperator Expression)*
ComparisonExpression ::= AdditiveExpression (ComparisonOperator Expression)*
EqualityExpression ::= ComparisonExpression (EqualityOperator Expression)*
ConjunctionExpression ::= EqualityExpression ("and" Expression)*
DisjunctionExpression ::= ConjunctionExpression ("or" Expression)*
ConcatenationExpression ::= DisjunctionExpression ("&" Expression)*

Expression ::= ConcatenationExpression
                | ("-" | "not")? "(" Expression ")"
                | FunctionInvocationExpression
                | CallbackExpression
                | IndexExpression
                | IfThenElseExpression
                | MemberAccessExpression


```

![expression](readme_images/railroad_expression.png)