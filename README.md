<!-- This file is rendered by https://github.com/BlvckBytes/readme_helper -->

# GPEEE

![build](https://github.com/BlvckBytes/GPEEE/actions/workflows/build.yml/badge.svg)
[![coverage](https://codecov.io/gh/BlvckBytes/GPEEE/branch/main/graph/badge.svg?token=WGWX8IDT00)](https://codecov.io/gh/BlvckBytes/GPEEE)

![logo](readme_images/logo.png)

The opensource `General Purpose Environment Expression Evaluator` (GPEEE) which you most definitely
want to integrate into your next project.

## Table of Contents
- [Mission Statement](#mission-statement)
- [Getting Started](#getting-started)
  - [Installation](#installation)
  - [Evaluator Instance](#evaluator-instance)
  - [Creating An Environment](#creating-an-environment)
  - [Function Implementation Example](#function-implementation-example)
  - [Full Use Example](#full-use-example)
- [Syntax](#syntax)
  - [Multiline Programs](#multiline-programs)
  - [Operator Precedence](#operator-precedence)
  - [Optional Access](#optional-access)
    - [Members](#members)
    - [Indexing](#indexing)
    - [Function Calls](#function-calls)
    - [Null Coalescence](#null-coalescence)
  - [Primary Expressions](#primary-expressions)
  - [Grammar Definition](#grammar-definition)

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
* Implement all generally known and loved operators but **don't** add anything fancy
* Expressions will **not** keep any state between evaluations
* Next to basic terminal values, operators and if-then-else, there will be **no** keywords
* **All** remaining control flow and complex logic will be outsourced to Java and hidden behind functions

Without adhering to these, I'm sure the project would grow into an unmaintainable beast of "one more
feature please". The restrictions may seem harsh, but you're still able to get up and running with
all customizations by defining your own *evaluation environment* exactly to your needs in no time.

## Getting Started

### Installation

For now, this resource won't be hosted anywhere. Please clone the last successfully built commit,
navigate into it and run `mvn install` to install it into your local maven repository.

Then, add the project as a dependency to your new project:

```xml
<dependency>
  <groupId>me.blvckbytes</groupId>
  <artifactId>GPEEE</artifactId>

  <!-- Use whatever the pom.xml of GPEEE currently specifies -->
  <version>0.1-SNAPSHOT</version>
</dependency>
```

As soon as this software has proven itself useful, releases and hosted dependencies will be set up.

### Evaluator Instance

The working principle of this evaluator is as simple as it gets: You provide an *evaluation environment*
as well as a string containing the target expression and the evaluator returns an *evaluation result* or
throws an error to be properly handled by the caller.

<details>
<summary>IExpressionEvaluator.java</summary>

```java
package me.blvckbytes.gpeee;

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
   * Optimizes the provided expression by collapsing static expressions into their result.
   * This only makes sense if the target expression is being evaluated more than once.
   * @param expression Expression to optimize
   * @return Optimized expression
   * @throws AEvaluatorError Error during the evaluation process
   */
  AExpression optimizeExpression(AExpression expression) throws AEvaluatorError;

  /**
   * Evaluates a previously parsed expression within a provided evaluation environment.
   * @param expression Expression to evaluate
   * @return Resulting expression value
   * @throws AEvaluatorError Error during the interpretation process
   */
  Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError;

}
```
</details>


In order to allow for pre-parsed expressions which can be evaluated over and over again while their environment
may change it's values, the parsing, evaluating and optimizing stages have been strictly separated. Generally
speaking, you do **not** want to optimize only once evaluated expressions, but most definitely want to do so if
they're used multiple times.

This interface has been implemented by the `me.blvckbytes.gpeee.GPEEE` class, which can be easily
instantiated by providing an optional logger to it's constructor. As soon as you got an instance, you can evaluate
as many expressions in as many environments with it as you'd like.

### Creating An Environment

The evaluation environment tells the interpreter of your expressions which variables and which functions are available
and may be substituted/called from within an expression and how different types of values are to be handled.

<details>
<summary>IEvaluationEnvironment.java</summary>

```java
package me.blvckbytes.gpeee.interpreter;

public interface IEvaluationEnvironment {

  /**
   * Mapping identifiers to available functions which an expression may invoke
   */
  Map<String, AExpressionFunction> getFunctions();

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
   * operation on them which they'd usually not support naturally. Provide null
   * in order to use the standard interpreter.
   *
   * Most of the time, you'd want to provide {@link me.blvckbytes.gpeee.GPEEE#STD_VALUE_INTERPRETER}
   */
  IValueInterpreter getValueInterpreter();

}
```
</details>


If a value is going to be constant throughout the lifetime of an environment, you may use a static variable. Otherwise,
it's advised to not update the map over and over again, but to rather specify a live variable supplier. This supplier
will be called whenever the interpreter needs this variable's value.

The *value interpreter* is used to define how different data-types can be interpreted and combined using various
operations. Implementing such an instance can take a lot of time and is prone to errors, which is why there's a very
sensible default implementation which' use is recommended, as described in the comment inside the above interface.

In order to create a new expression function, it's best practice to create a separate class which inherits the
following abstract base class:

<details>
<summary>AExpressionFunction.java</summary>

```java
package me.blvckbytes.gpeee.functions;

public abstract class AExpressionFunction {

  //=========================================================================//
  //                             Abstract Methods                            //
  //=========================================================================//

  /**
   * Called whenever a function call to the registered corresponding
   * identifier is performed within an expression
   * @param environment A reference to the current environment
   * @param args Arguments supplied by the invocation
   * @return Return value of this function
   */
  public abstract Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args);

  /**
   * Specifies the function arguments in order and how they have to be used in
   * order to make up a valid function call. If this method returns null,
   * the function call will not be checked.
   */
  public abstract @Nullable List<ExpressionFunctionArgument> getArguments();

  //=========================================================================//
  //                                  Utilities                              //
  //=========================================================================//

  /**
   * Get a known non-null argument from the argument list
   * @param args Argument list to read from
   * @param index Index of the known argument
   * @return Argument, required to be non-null
   */
  @SuppressWarnings("unchecked")
  protected<T> T nonNull(List<@Nullable Object> args, int index) {
    return (T) Objects.requireNonNull(args.get(index));
  }

  /**
   * Get a maybe null argument from the argument list
   * @param args Argument list to read from
   * @param index Index of the argument
   * @return Argument from the list or null if the index has been out-of-range
   */
  @SuppressWarnings("unchecked")
  protected<T> @Nullable T nullable(List<@Nullable Object> args, int index) {
    return (T) (index >= args.size() ? null : args.get(index));
  }

  //=========================================================================//
  //                               Internal API                              //
  //=========================================================================//

  /**
   * Validates the provided list of arguments against the locally kept argument definitions
   * and throws a detailed {@link InvalidFunctionArgumentTypeError} when an argument mismatches.
   * @param expression Expression for error context
   * @param valueInterpreter Reference to the currently in-use value interpreter for possible auto-conversions
   * @param args Arguments to validate
   * @throws InvalidFunctionArgumentTypeError Thrown when an argument mismatches it's corresponding definition
   */
  public void validateArguments(FunctionInvocationExpression expression, IValueInterpreter valueInterpreter, List<@Nullable Object> args) throws InvalidFunctionArgumentTypeError {
    List<ExpressionFunctionArgument> argumentDefinitions = getArguments();

    // No definitions available, cannot validate, call passes
    if (argumentDefinitions == null)
      return;

    // Check all arguments one by one in order
    for (int i = 0; i < argumentDefinitions.size(); i++) {
      ExpressionFunctionArgument definition = argumentDefinitions.get(i);
      Object argument = i >= args.size() ? null : args.get(i);
      Tuple<Boolean, @Nullable Object> result = definition.checkDescriptionAndPossiblyConvert(argument, valueInterpreter);

      // Value did not pass all checks and could not be auto-converted either
      if (!result.getA())
        throw new InvalidFunctionArgumentTypeError(expression, definition, i, argument);

      // Update the value within the list to the possibly converted value
      if (i < args.size())
        args.set(i, result.getB());
    }
  }
}
```
</details>


If argument validation is not required, the argument list getter can always just return null. Otherwise, arguments
may be specified in order, where non-required (non-positional) arguments may only follow after all required arguments. An
argument consists of it's name, description, required-flag as well as an optional list of accepted types. Leave this
argument empty to not type-check at all.

<details>
<summary>AExpressionFunction.java</summary>

```java
package me.blvckbytes.gpeee.functions;

public abstract class AExpressionFunction {

  //=========================================================================//
  //                             Abstract Methods                            //
  //=========================================================================//

  /**
   * Called whenever a function call to the registered corresponding
   * identifier is performed within an expression
   * @param environment A reference to the current environment
   * @param args Arguments supplied by the invocation
   * @return Return value of this function
   */
  public abstract Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args);

  /**
   * Specifies the function arguments in order and how they have to be used in
   * order to make up a valid function call. If this method returns null,
   * the function call will not be checked.
   */
  public abstract @Nullable List<ExpressionFunctionArgument> getArguments();

  //=========================================================================//
  //                                  Utilities                              //
  //=========================================================================//

  /**
   * Get a known non-null argument from the argument list
   * @param args Argument list to read from
   * @param index Index of the known argument
   * @return Argument, required to be non-null
   */
  @SuppressWarnings("unchecked")
  protected<T> T nonNull(List<@Nullable Object> args, int index) {
    return (T) Objects.requireNonNull(args.get(index));
  }

  /**
   * Get a maybe null argument from the argument list
   * @param args Argument list to read from
   * @param index Index of the argument
   * @return Argument from the list or null if the index has been out-of-range
   */
  @SuppressWarnings("unchecked")
  protected<T> @Nullable T nullable(List<@Nullable Object> args, int index) {
    return (T) (index >= args.size() ? null : args.get(index));
  }

  //=========================================================================//
  //                               Internal API                              //
  //=========================================================================//

  /**
   * Validates the provided list of arguments against the locally kept argument definitions
   * and throws a detailed {@link InvalidFunctionArgumentTypeError} when an argument mismatches.
   * @param expression Expression for error context
   * @param valueInterpreter Reference to the currently in-use value interpreter for possible auto-conversions
   * @param args Arguments to validate
   * @throws InvalidFunctionArgumentTypeError Thrown when an argument mismatches it's corresponding definition
   */
  public void validateArguments(FunctionInvocationExpression expression, IValueInterpreter valueInterpreter, List<@Nullable Object> args) throws InvalidFunctionArgumentTypeError {
    List<ExpressionFunctionArgument> argumentDefinitions = getArguments();

    // No definitions available, cannot validate, call passes
    if (argumentDefinitions == null)
      return;

    // Check all arguments one by one in order
    for (int i = 0; i < argumentDefinitions.size(); i++) {
      ExpressionFunctionArgument definition = argumentDefinitions.get(i);
      Object argument = i >= args.size() ? null : args.get(i);
      Tuple<Boolean, @Nullable Object> result = definition.checkDescriptionAndPossiblyConvert(argument, valueInterpreter);

      // Value did not pass all checks and could not be auto-converted either
      if (!result.getA())
        throw new InvalidFunctionArgumentTypeError(expression, definition, i, argument);

      // Update the value within the list to the possibly converted value
      if (i < args.size())
        args.set(i, result.getB());
    }
  }
}
```
</details>


This class will also itself try to convert passed values to the required type before letting the interpreter throw a
mismatch error by making use of the *value interpreter*.

### Function Implementation Example

There are a few standard (std) functions already included in this software package you can take a look at in order
to get a feel for how functions are intended to be used and created. As an example, an excerpt from the std function
`iter_cat` has been provided for you.

<details>
<summary>IterCatFunction.java</summary>

```java
package me.blvckbytes.gpeee.functions.std;

public class IterCatFunction extends AStandardFunction {

  @Override
  public Object apply(IEvaluationEnvironment env, List<@Nullable Object> args) {
    // Retrieve arguments
    Collection<?> items = nonNull(args, 0);
    AExpressionFunction mapper = nonNull(args, 1);
    @Nullable String separator = nullable(args, 2);
    @Nullable String fallback = nullable(args, 3);

    // Fall back on a sensible default
    if (separator == null)
      separator = ", ";

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
      new ExpressionFunctionArgument("items",     "Collection to iterate",             true,  Collection.class),
      new ExpressionFunctionArgument("mapper",    "Iteration item mapper function",    true,  AExpressionFunction.class),
      new ExpressionFunctionArgument("separator", "Item separator",                    false, String.class),
      new ExpressionFunctionArgument("fallback",  "Fallback when collection is empty", false, String.class)
    );
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("iter_cat", this);
  }

  @Override
  public boolean returnsPrimaryResult() {
    return true;
  }
}
```
</details>


While this implementation is a standard function, the exact same way of implementing functions applies to custom functions, 
minus the registration- as well as the result return boolean parts.

### Full Use Example

The following class shows a compact but complete use-case of the `GPEEE`.

<details>
<summary>FullUseExample.java</summary>

```java
package me.blvckbytes.gpeee;

public class FullUseExample {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static void main(String[] args) {
    try {

      ILogger logger = new ILogger() {
        @Override
        public void logDebug(ILogSourceType source, String message) {
          System.out.println("[DEBUG] [" + source.name() + "]: " + message);
        }

        @Override
        public void logError(String message, @Nullable Exception error) {
          System.err.println(message);

          if (error != null)
            error.printStackTrace();
        }
      };

      GPEEE evaluator = new GPEEE(logger);

      AExpression expr = evaluator.parseString("5 * 3 - 2 & \" Hello, world! \" & current_time");
      System.out.println("unoptimized expression: " + expr.expressionify());
      System.out.println("unoptimized AST: " + expr.stringify(" ", 0));

      expr = evaluator.optimizeExpression(expr);
      System.out.println("optimized expression: " + expr.expressionify());
      System.out.println("optimized AST: " + expr.stringify(" ", 0));

      IEvaluationEnvironment env = new IEvaluationEnvironment() {

        @Override
        public Map<String, AExpressionFunction> getFunctions() {
          // Register your functions here
          return Map.of();
        }

        @Override
        public Map<String, Supplier<Object>> getLiveVariables() {
          // Register your live variables here
          return Map.of(
            "current_time", () -> DATE_FORMAT.format(new Date())
          );
        }

        @Override
        public Map<String, Object> getStaticVariables() {
          // Register your static variables here
          return Map.of();
        }

        @Override
        public IValueInterpreter getValueInterpreter() {
          // It's advised to just use the standard value interpreter
          return GPEEE.STD_VALUE_INTERPRETER;
        }
      };

      // Prints: 13 Hello, world! 2022-12-21 18:29:47
      System.out.println("result=" + evaluator.evaluateExpression(expr, env));
    }
    catch (AEvaluatorError e) {
      // The message of an AEvaluatorError always contains an excerpt of the input expression
      // as well as a marker with an explanation below it, which is why it's recommended to
      // print it's message separately to not distort it's formatting
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
```
</details>


## Syntax

The syntax of this language has been mostly inspired by *JavaScript* as well as *Python* and is easy to grow accustomed
to once you've spent a few minutes to experiment with it.

### Multiline Programs

A program may only consist of a single expression if that's sufficient for the use-case at hand, but it may also involve
multiple expressions which get processed top-down. For example: One expression can assign a variable which then can be
accessed by another expression later on.

The following program represents a really simple example:

```
add_prefix = (input) => "prefix: " & input
add_prefix("Hello") & ", " & add_prefix("World")
```

Which results in: `prefix: Hello, prefix: World`

This functionality can be useful to extract reused expressions for a single program. If you notice similar patterns across
multiple programs, it would be advised to rather create a function in Java, which will then be accessible to all programs.

### Operator Precedence

This operator table lists all available operators as well as their precedence when evaluating expressions. A lower
precedence number means that the value of that expression is evaluated first.

| Operator                            | Name                           | Example                                          | Precedence |
|-------------------------------------|--------------------------------|--------------------------------------------------|------------|
| (...)                               | Parentheses                    | (5 + 3) * 2                                      | 0          |
| (...) => ...                        | Callback                       | (a, b) => a + b                                  | 1          |
| if ... then ... else ...            | If-Then-Else                   | if a > 5 then b else c                           | 2          |
| my_function(...), my_function?(...) | (Optional) Function Invocation | map_of("k1", "v1", "k2", "v2")                   | 3          |
| (...).(...), (...)?.(...)           | (Optional) Member Access       | a.b, a?.b?.c, a?.(my_field_name_expr)            | 4          |
| (...)\[(...)\], (...)?\[(...)\]     | (Optional) Indexing            | a\[0\], a?\["key"\]?\[c\], a?\[(my_index_expr)\] | 5          |
| -(...)                              | Flip Sign                      | -a, -5, -my_function(), 2^-(1/2)                 | 6          |
| not (...)                           | Negation                       | not a, not my_function(), not a or b             | 7          |
| (...) ^ (...)                       | Exponentiation                 | a^2, (a+b)^(c-d)                                 | 8          |
| (...) * (...)                       | Multiplication                 | a * 2, (a+b) * (c-d)                             | 9          |
| (...) / (...)                       | Division                       | a / b, (a+b) / (c-d)                             | 9          |
| (...) % (...)                       | Modulo                         | a % b, (a+b) % (c-d)                             | 9          |
| (...) + (...)                       | Addition                       | a + b                                            | 10         |
| (...) - (...)                       | Subtraction                    | a - b, (a+b) - (a-b)                             | 10         |
| (...) > (...)                       | Greater Than                   | a > b                                            | 11         |
| (...) < (...)                       | Less Than                      | a < b                                            | 11         |
| (...) >= (...)                      | Greater Than Or Equal          | a >= b                                           | 11         |
| (...) <= (...)                      | Less Than Or Equal             | a <= b                                           | 11         |
| (...) == (...)                      | Equals                         | a == b                                           | 12         |
| (...) != (...)                      | Not Equals                     | a != b                                           | 12         |
| (...) === (...)                     | Equals Exact                   | a === b                                          | 12         |
| (...) !== (...)                     | Not Equals Exact               | a !== b                                          | 12         |
| (...) and (...)                     | Boolean Conjunction            | a and b                                          | 13         |
| (...) or (...)                      | Boolean Disjunction            | a or b                                           | 14         |
| (...) & (...)                       | Concatenation                  | a & b                                            | 15         |
| (...) ?? (...)                      | Null Coalescence               | a ?? b                                           | 16         |
| my_variable = (...)                 | Simple Assignment              | a = 5                                            | 17         |

### Optional Access

While all other operators should be pretty self-explanatory, some of their optional access versions
might need a little description of themselves. They can be made use of whenever it's preferred to not
throw errors on invalid access, but rather just return null.

#### Members

If a variable value is an object and thus contains fields, these fields can be accessed by the member access
operator. As long as the requested field name exists, it's value will be substituted properly. Otherwise, an
error will be thrown. The optional member access operator can be used and even chained together by just prepending
a question-mark to the operator itself.

```
a?.b?.c?.d
```

#### Indexing

If a variable value is either of type map and thus contains key-value pairs or of type list/array which contains
numerically indexed values, all of these values can be accessed by their key. As long as the requested key exists,
it's value will be substituted properly. Otherwise, an error will be thrown. The optional indexing can be used and
even chained together by just prepending a question-mark to the opening bracket itself.

```
my_map?["key1"]?[0]?[1]
```

#### Function Calls

If a function exists within the environment of an evaluation, it may be called an arbitrary number of times within the
expression itself. To just receive a null-value whenever the target function isn't available, a question-mark can be
prepended to the opening parenthesis of the call notation.

```
my_function?()
```

#### Null Coalescence

While this operator is often used in combination with optional access operators, it also makes a lot of sense to be used
on it's own. It checks whether the left-hand-side value is null. If it is, it returns it's right-hand-side value, otherwise
the left-hand-side value will be just passed through without any modifications.

```
my_maybe_null ?? "Fallback value"
```

### Primary Expressions

A primary expression is an immediate value and the most simple type of expression possible. These types of primary expressions are
available:

| Name          | Example                     | Description                          |
|---------------|-----------------------------|--------------------------------------|
| Literal True  | true                        | A positive boolean value             |
| Literal False | false                       | A negative boolean value             |
| Literal Null  | null                        | The null value                       |
| Double        | 12.3, .4, -.8, -1           | A non-whole number                   |
| Long          | 123, 4, -8, -1              | A whole number                       |
| String        | "my string", "my \\" quote" | An immediate string of characters    |
| Identifier    | a, my_var, my_func          | Either a variable or a function name |

### Grammar Definition

The following *EBNF* describes the grammar of this small expression language precisely:

<details>
<summary>grammar.ebnf</summary>

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

NullCoalesceExpression ::= ConcatenationExpression ("??" ConcatenationExpression)*
ConcatenationExpression ::= DisjunctionExpression ("&" DisjunctionExpression)*
DisjunctionExpression ::= ConjunctionExpression ("or" ConjunctionExpression)*
ConjunctionExpression ::= EqualityExpression ("and" EqualityExpression)*
EqualityExpression ::= ComparisonExpression (EqualityOperator ComparisonExpression)*
ComparisonExpression ::= AdditiveExpression (ComparisonOperator AdditiveExpression)*
AdditiveExpression ::= MultiplicativeExpression (AdditiveOperator MultiplicativeExpression)*
MultiplicativeExpression ::= ExponentiationExpression (MultiplicativeOperator ExponentiationExpression)*
ExponentiationExpression ::= NegationExpression ("^" NegationExpression)*

NegationExpression ::= FlipSignExpression ("-" FlipSignExpression)?
FlipSignExpression ::= IndexExpression ("not" IndexExpression)?

IndexExpression ::= MemberAccessExpression (("[" | "?[") Expression "]")*
MemberAccessExpression ::= FunctionInvocationExpression (("." | "?.") FunctionInvocationExpression)*

FunctionArgument ::= (Identifier "=")? Expression
FunctionInvocationExpression ::= (Identifier ("(" | "?(") (FunctionArgument | (FunctionArgument ("," FunctionArgument)*))? ")") | IfThenElseExpression

IfThenElseExpression ::= ("if" Expression "then" Expression "else" Expression) | CallbackExpression
CallbackExpression ::= ("(" (Identifier | (Identifier ("," Identifier)*)) ")" "=>" Expression) | ParenthesesExpression
ParenthesesExpression ::= ("(" Expression ")") | PrimaryExpression

PrimaryExpression ::= Long | Double | String | Identifier | Literal

Expression ::= NullCoalesceExpression
ProgramExpression ::= Expression+
```
</details>


