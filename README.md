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
- [Standard Functions](#standard-functions)
  - [bool](#bool)
  - [date_format](#date_format)
  - [iter_cat](#iter_cat)
  - [key](#key)
  - [len](#len)
  - [l_index](#l_index)
  - [list](#list)
  - [list_of](#list_of)
  - [map](#map)
  - [map_of](#map_of)
  - [print](#print)
  - [r_index](#r_index)
  - [split](#split)
  - [str](#str)
  - [substring](#substring)
  - [title_case](#title_case)
  - [value](#value)
  - [range](#range)
  - [flatten](#flatten)
  - [min](#min)
  - [max](#max)

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
  Map<String, Supplier<?>> getLiveVariables();

  /**
   * Mapping identifiers to available static variables which an expression may resolve
   */
  Map<String, ?> getStaticVariables();

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

  /**
   * Get a maybe null argument from the argument list or use the provided fallback
   * @param args Argument list to read from
   * @param index Index of the argument
   * @param fallback Fallback to use in the situation of an absent argument
   * @return Argument from the list or null if the index has been out-of-range
   */
  @SuppressWarnings("unchecked")
  protected<T> T nullableWithFallback(List<@Nullable Object> args, int index, T fallback) {
    T result = (T) (index >= args.size() ? fallback : args.get(index));

    if (result == null)
      return fallback;

    return result;
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

  /**
   * Get a maybe null argument from the argument list or use the provided fallback
   * @param args Argument list to read from
   * @param index Index of the argument
   * @param fallback Fallback to use in the situation of an absent argument
   * @return Argument from the list or null if the index has been out-of-range
   */
  @SuppressWarnings("unchecked")
  protected<T> T nullableWithFallback(List<@Nullable Object> args, int index, T fallback) {
    T result = (T) (index >= args.size() ? fallback : args.get(index));

    if (result == null)
      return fallback;

    return result;
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
    @Nullable String separator = nullableWithFallback(args, 2, ", ");
    @Nullable String fallback = nullable(args, 3);

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
    // iter_cat(items, (it, ind) => (..), "separator", "no items fallback")
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
        public Map<String, Supplier<?>> getLiveVariables() {
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


## Standard Functions

Standard functions are functions which are always going to be available, no matter of the current environment. They cannot be shadowed
by environment identifiers and provide basic features which you're likely going to need if you're notating logic.

For the sake of readability, functions are notated in `TypeScript` notation within this list of functions. The type follows after
the colon (`:`) and a question mark (`?`) signals an optional input. In order to help you to understand their behaviour, their
test cases have been added in an expandable container, which provide use-case examples.

### bool

Interpret the input value as a boolean by making use of the environments value interpreter.

| Argument | Description                     |
|----------|---------------------------------|
| input    | Value to interpret as a boolean |

```
bool(input?: Object): Boolean
```

<details>
<summary>BoolFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class BoolFunctionTests {

  @Test
  public void shouldInterpretValuesAsABoolean() {
    new EnvironmentBuilder()
      .withStaticVariable("my_list", List.of(1))
      .withStaticVariable("my_list_empty", List.of())
      .withStaticVariable("my_map", Map.of("k", "v"))
      .withStaticVariable("my_map_empty", Map.of())
      .launch(validator -> {
        validator.validate("bool(0)", false);
        validator.validate("bool(1)", true);
        validator.validate("bool(100)", true);
        validator.validate("bool(-1)", false);
        validator.validate("bool(-100)", false);

        validator.validate("bool(1.1)", true);
        validator.validate("bool(100.1)", true);
        validator.validate("bool(-1.1)", false);
        validator.validate("bool(-100.1)", false);

        validator.validate("bool(\"\")", false);
        validator.validate("bool(\"non-empty\")", true);

        validator.validate("bool(null)", false);
        validator.validate("bool(true)", true);
        validator.validate("bool(false)", false);

        validator.validate("bool(my_list)", true);
        validator.validate("bool(my_map)", true);
        validator.validate("bool(my_list_empty)", false);
        validator.validate("bool(my_map_empty)", false);
      });
  }
}
```
</details>


### date_format

Format dates with a specified format by making use of the specified time-zone offset.

| Argument | Description                                                                                                  |
|----------|--------------------------------------------------------------------------------------------------------------|
| date     | Date value to format                                                                                         |
| type     | Type of the provided date value                                                                              |
| format   | [Format](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) to apply when formatting |
| timezone | Timezone to use, defaults to UTC                                                                             |

The following `type` variations are currently available:

| type    | Description                    |
|---------|--------------------------------|
| seconds | Unix timestamp in seconds      |
| millis  | Unix timestamp in milliseconds |
| date    | Java Date Object               |

```
date_format(date: Number|Date, type: String, format: String, timezone?: String): String
```

<details>
<summary>DateFormatFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class DateFormatFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("date_format()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("date_format(0)", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("date_format(0, \"\")", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldThrowOnNonNumericValues() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .launch(validator -> {
        // Required, thus non-nullable
        validator.validateThrows("date_format(null, \"seconds\", format_a)", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("date_format(null, \"millis\", format_a)", InvalidFunctionArgumentTypeError.class);

        validator.validateThrows("date_format(\"\", \"seconds\", format_a)", InvalidFunctionInvocationError.class);
        validator.validateThrows("date_format(true, \"seconds\", format_a)", InvalidFunctionInvocationError.class);
        validator.validateThrows("date_format(\"\", \"millis\", format_a)", InvalidFunctionInvocationError.class);
        validator.validateThrows("date_format(true, \"millis\", format_a)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnNonMalformedFormat() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "hello, world")
      .launch(validator -> {
        validator.validateThrows("date_format(0, \"millis\", format)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnMalformedTimeZone() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "HH:mm")
      .withStaticVariable("zone", "hello")
      .launch(validator -> {
        validator.validateThrows("date_format(0, \"millis\", format, zone)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnMalformedType() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "HH:mm")
      .launch(validator -> {
        validator.validateThrows("date_format(0, \"hello\", format)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnNonDateObject() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "HH:mm")
      .withStaticVariable("non_date", new Object())
      .launch(validator -> {
        validator.validateThrows("date_format(non_date, \"date\", format)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldFormatSeconds() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .withStaticVariable("format_b", "yyyy/MM/dd")
      .withStaticVariable("format_c", "dd.MM.yyyy HH:mm:ss")
      .withStaticVariable("stamp", 1677579422) // Tue Feb 28 2023 10:17:02 UTC
      .launch(validator -> {
        validator.validate("date_format(stamp, \"seconds\", format_a)", "2023-02-28");
        validator.validate("date_format(stamp, \"seconds\", format_b)", "2023/02/28");
        validator.validate("date_format(stamp, \"seconds\", format_c)", "28.02.2023 10:17:02");

        validator.validate("date_format(stamp, \"seconds\", format_c, \"CET\")", "28.02.2023 11:17:02");
      });
  }

  @Test
  public void shouldFormatMilliSeconds() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .withStaticVariable("format_b", "yyyy/MM/dd")
      .withStaticVariable("format_c", "dd.MM.yyyy HH:mm:ss")
      .withStaticVariable("stamp", 1677579422L * 1000) // Tue Feb 28 2023 10:17:02 UTC
      .launch(validator -> {
        validator.validate("date_format(stamp, \"millis\", format_a)", "2023-02-28");
        validator.validate("date_format(stamp, \"millis\", format_b)", "2023/02/28");
        validator.validate("date_format(stamp, \"millis\", format_c)", "28.02.2023 10:17:02");

        validator.validate("date_format(stamp, \"millis\", format_c, \"CET\")", "28.02.2023 11:17:02");
      });
  }

  @Test
  public void shouldFormatDates() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .withStaticVariable("format_b", "yyyy/MM/dd")
      .withStaticVariable("format_c", "dd.MM.yyyy HH:mm:ss")
      .withStaticVariable("date", new Date(1677579422L * 1000)) // Tue Feb 28 2023 10:17:02 UTC
      .launch(validator -> {
        validator.validate("date_format(date, \"date\", format_a)", "2023-02-28");
        validator.validate("date_format(date, \"date\", format_b)", "2023/02/28");
        validator.validate("date_format(date, \"date\", format_c)", "28.02.2023 10:17:02");

        validator.validate("date_format(date, \"date\", format_c, \"CET\")", "28.02.2023 11:17:02");
      });
  }
}
```
</details>


### iter_cat

Iterate over a collection while mapping each iteration through a lambda function, who's result
is being appended to the final result string.

| Argument  | Description                                                |
|-----------|------------------------------------------------------------|
| items     | Collection to iterate                                      |
| mapper    | Lambda function to map items with                          |
| separator | Separator to use when concatenating items, defaults to "," |
| fallback  | Value to return if the collection is empty                 |

```
iter_cat(items: Collection<?>, mapper: (item: Object, index: Number) => String, separator?: String, fallback?: Object): String
```

<details>
<summary>IterCatFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class IterCatFunctionTests {

  @Test
  public void shouldThrowOnNonCollectionInput() {
    createEnvironment().launch(validator -> {
      validator.validateThrows("iter_cat(my_number, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("iter_cat(my_string, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("iter_cat(my_boolean, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("iter_cat(null, () => \"\")", InvalidFunctionArgumentTypeError.class);
    });
  }

  @Test
  public void shouldAcceptAMapAsInput() {
    createEnvironment().launch(validator -> {
      validator.validate(
        "iter_cat(my_map, (it, ind) => \"(\" & ind & \" -> \" & key(it) & \"-\" & value(it) & \")\", \", \")",
        "(0 -> red-#FF0000), (1 -> green-#00FF00), (2 -> blue-#0000FF)"
      );
    });
  }

  @Test
  public void shouldAcceptAListAsInput() {
    createEnvironment().launch(validator -> {
      validator.validate(
        "iter_cat(my_list, (it, ind) => \"(\" & ind & \" -> \" & it & \")\", \" | \")",
        "(0 -> red) | (1 -> green) | (2 -> blue)"
      );
    });
  }

  @Test
  public void shouldUseDefaultSeparator() {
    createEnvironment().launch(validator -> {
      validator.validate(
        "iter_cat(my_list, (it, ind) => \"(\" & ind & \" -> \" & it & \")\")",
        "(0 -> red), (1 -> green), (2 -> blue)"
      );
    });
  }

  @Test
  public void shouldPrintEmptyIfEmptyAndNoFallbackAvailable() {
    createEnvironment().launch(validator -> {
      validator.validate(
        "iter_cat(my_list_empty, (it, ind) => \"(\" & ind & \" -> \" & it & \")\")",
        ""
      );
    });
  }

  @Test
  public void shouldPrintFallbackIfEmptyAndFallbackAvailable() {
    createEnvironment().launch(validator -> {
      validator.validate(
        "iter_cat(my_list_empty, (it, ind) => \"(\" & ind & \" -> \" & it & \")\", fallback=\"this is my fallback\")",
        "this is my fallback"
      );
    });
  }

  private EnvironmentBuilder createEnvironment() {
    return new EnvironmentBuilder()
      .withStaticVariable("my_list", createColorList())
      .withStaticVariable("my_list_empty", List.of())
      .withStaticVariable("my_map", createColorMap())
      .withStaticVariable("my_map_empty", Map.of())
      .withStaticVariable("my_number", 1)
      .withStaticVariable("my_string", "hello world")
      .withStaticVariable("my_boolean", true);
  }

  private List<String> createColorList() {
    return List.of("red", "green", "blue");
  }

  private Map<String, String> createColorMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("red", "#FF0000");
    map.put("green", "#00FF00");
    map.put("blue", "#0000FF");
    return map;
  }
}
```
</details>


### key

Extracts the key from a Java `Map.Entry<?, ?>`.

| Argument | Description           |
|----------|-----------------------|
| entry    | Entry to extract from |

```
key(entry: Map.Entry<?, ?>): Object
```

<details>
<summary>KeyFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class KeyFunctionTests {

  @Test
  public void shouldThrowOnNonMapEntryInput() {
    createEnvironment().launch(validator -> {
      validator.validateThrows("key(my_number, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("key(my_string, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("key(my_boolean, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("key(my_list, () => \"\")", InvalidFunctionArgumentTypeError.class);
    });
  }

  @Test
  public void shouldReturnMapKeys() {
    createEnvironment().launch(validator -> {
      validator.validate("key(list(my_map)[0])", "red");
      validator.validate("key(list(my_map)[1])", "green");
      validator.validate("key(list(my_map)[2])", "blue");
      validator.validate("key(null)", (Object) null);
    });
  }

  private EnvironmentBuilder createEnvironment() {
    return new EnvironmentBuilder()
      .withStaticVariable("my_list", createColorList())
      .withStaticVariable("my_list_empty", List.of())
      .withStaticVariable("my_map", createColorMap())
      .withStaticVariable("my_map_empty", Map.of())
      .withStaticVariable("my_number", 1)
      .withStaticVariable("my_string", "hello world")
      .withStaticVariable("my_boolean", true);
  }

  private List<String> createColorList() {
    return List.of("red", "green", "blue");
  }

  private Map<String, String> createColorMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("red", "#FF0000");
    map.put("green", "#00FF00");
    map.put("blue", "#0000FF");
    return map;
  }
}
```
</details>


### len

Returns the length of the provided value, based on it's type.

| Argument | Description                |
|----------|----------------------------|
| input    | Value to get the length of |

```
len(value: Object): Number
```

Where the following value types are supported

| Input Type    | Description              |
|---------------|--------------------------|
| null, default | Always returns 0         |
| String        | Length of the string     |
| Collection<?> | Length of the collection |
| Map<?, ?>     | Length of the map        |
| Array         | Length of the array      |

<details>
<summary>LenFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class LenFunctionTests {

  @Test
  public void shouldRespondWithAnItemsLength() {
    new EnvironmentBuilder()
      .withStaticVariable("my_list", List.of(1, 2, 3, 4))
      .withStaticVariable("my_list_empty", List.of())
      .withStaticVariable("my_map", Map.of("k", "v", "k2", "v2"))
      .withStaticVariable("my_map_empty", Map.of())
      .withStaticVariable("my_array", new int[] { 1, 2, 3 })
      .withStaticVariable("my_array_empty", new int[] {})
      .withStaticVariable("my_string", "hello, world")
      .launch(validator -> {
        validator.validate("len(my_list)", 4);
        validator.validate("len(my_list_empty)", 0);
        validator.validate("len(my_map)", 2);
        validator.validate("len(my_map_empty)", 0);
        validator.validate("len(my_array)", 3);
        validator.validate("len(my_array_empty)", 0);
        validator.validate("len(my_string)", "hello, world".length());
        validator.validate("len(null)", 0);
        validator.validate("len(1)", 0);
        validator.validate("len(1.1)", 0);
      });
  }
}
```
</details>


### l_index

Returns the first index of the passed substring within the input string. Returns -1 if the searched
string is not at all present in the input string.

| Argument | Description          |
|----------|----------------------|
| input    | Input to search in   |
| search   | String to search for |

```
l_index(input: String, search: String): Number
```

<details>
<summary>LIndexFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class LIndexFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("l_index()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("l_index(\"\")", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("l_index(null, null)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldReturnFirstIndexOf() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("l_index(\"hello, world\", \"h\")", 0);
        validator.validate("l_index(\"hello, world\", \"l\")", 2);
        validator.validate("l_index(\"hello, world\", \"x\")", -1);
      });
  }
}
```
</details>


### list

Interpret the input value as a list. Scalar values will create singleton lists, lists will be passed through and
maps will be converted to lists of their entry-sets.

| Argument | Description                  |
|----------|------------------------------|
| input    | Input to interpret as a list |

```
list(input?: Object): List<?>
```

<details>
<summary>ListFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class ListFunctionTests {

  @Test
  public void shouldInterpretValuesAsAList() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_list", createList(1))
      .withStaticVariable("my_list_empty", createList())
      .withStaticVariable("my_map", Map.of("k", "v"))
      .withStaticVariable("my_map_empty", Map.of());

    env.launch(validator -> {
      validator.validate("list(0)", createList(0));
      validator.validate("list(1)", createList(1));
      validator.validate("list(100)", createList(100));
      validator.validate("list(-1)", createList(-1));
      validator.validate("list(-100)", createList(-100));

      validator.validate("list(1.1)", createList(1.1));
      validator.validate("list(100.1)", createList(100.1));
      validator.validate("list(-1.1)", createList(-1.1));
      validator.validate("list(-100.1)", createList(-100.1));

      validator.validate("list(\"\")", createList(""));
      validator.validate("list(\"non-empty\")", createList("non-empty"));

      validator.validate("list(null)", createList());
      validator.validate("list(true)", createList(true));
      validator.validate("list(false)", createList(false));

      // Lists should be passed through
      validator.validate("list(my_list)", env.getVariable("my_list"));
      validator.validate("list(my_list_empty)", env.getVariable("my_list_empty"));

      // Maps should be converted to their entry-set
      validator.validate("list(my_map)", ((Map<?, ?>) Objects.requireNonNull(env.getVariable("my_map"))).entrySet());
      validator.validate("list(my_map_empty)", ((Map<?, ?>) Objects.requireNonNull(env.getVariable("my_map_empty"))).entrySet());
    });
  }

  private List<Object> createList(Object... items) {
    return new ArrayList<>(Arrays.asList(items));
  }
}
```
</details>


### list_of

Create a list from a variable amount of scalar input values.

| Argument | Description                     |
|----------|---------------------------------|
| input... | Variable amount of input values |

```
list_of(value...?: Object): List<?>
```

<details>
<summary>ListOfFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class ListOfFunctionTests {

  @Test
  public void shouldCollectArgumentsIntoAList() {
    EnvironmentBuilder env = new EnvironmentBuilder();

    env.launch(validator -> {
      validator.validate("list_of(0)", List.of(0));
      validator.validate("list_of(0, 1, 2)", List.of(0, 1, 2));
      validator.validate("list_of(2, 3, \"String\")", List.of(2, 3, "String"));
      validator.validate("list_of()", List.of());
      validator.validate("list_of(null)", nullList());
    });
  }

  private List<Object> nullList() {
    List<Object> result = new ArrayList<>();
    result.add(null);
    return result;
  }
}
```
</details>


### map

Iterate over a collection while mapping each iteration through a lambda function, who's result
is being appended to the final result list.

| Argument  | Description                                                |
|-----------|------------------------------------------------------------|
| items     | Collection to iterate                                      |
| mapper    | Lambda function to map items with                          |
| fallback  | Value to return if the collection is empty                 |

```
map(items: Collection<?>, mapper: (item: Object, index: Number) => String, fallback?: Object): List<?>
```

<details>
<summary>MapFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class MapFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .withStaticVariable("items", List.of())
      .launch(validator -> {
        validator.validateThrows("map()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("map(items)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldReturnFallbackValueWhenEmpty() {
    new EnvironmentBuilder()
      .withStaticVariable("items_empty", List.of())
      .withStaticVariable("items_one", List.of(1))
      .launch(validator -> {
        validator.validate("map(items_empty, (item) => item, \"empty collection\")", List.of("empty collection"));
        validator.validate("map(items_one, (item) => item, \"empty collection\")", List.of(1));
      });
  }

  @Test
  public void shouldMapInputItems() {
    new EnvironmentBuilder()
      .withStaticVariable("items", List.of("a", "b", "c"))
      .withStaticVariable("items_empty", List.of())
      .launch(validator -> {
        validator.validate("map(items, (item) => item & \" suffix\")", List.of("a suffix", "b suffix", "c suffix"));
        validator.validate("map(items_empty, (item, index) => index & item)", List.of());
      });
  }
}
```
</details>


### map_of

Create a list from a variable amount of scalar input value pairs.

| Argument | Description                                     |
|----------|-------------------------------------------------|
| input... | Variable amount of input values, taken in pairs |

```
map_of(value...?: Object): Map<?, ?>
```

<details>
<summary>MapOfFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class MapOfFunctionTests {

  @Test
  public void shouldCollectArgumentsIntoAMap() {
    EnvironmentBuilder env = new EnvironmentBuilder();

    env.launch(validator -> {
      validator.validate("map_of(\"k\", 1)", Map.of("k", 1));
      validator.validate("map_of(\"k1\", 1.2, \"k2\", -5, \"k3\", \"value 3\")", Map.of("k1", 1.2, "k2", -5, "k3", "value 3"));
      validator.validate("map_of(\"k\", null)", nullMap("k"));
      validator.validate("map_of()", Map.of());

      validator.validateThrows("map_of(\"k\")", InvalidFunctionInvocationError.class);
      validator.validateThrows("map_of(\"k\", 1, \"k2\")", InvalidFunctionInvocationError.class);
    });
  }

  private Map<String, ?> nullMap(String key) {
    Map<String, ?> result = new HashMap<>();
    result.put(key, null);
    return result;
  }
}
```
</details>


### print

Print the input values to STDOUT.

| Argument | Description                     |
|----------|---------------------------------|
| input... | Variable amount of input values |

```
print(input...?: Object): void
```

<details>
<summary>PrintFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class PrintFunctionTests {

  @Test
  public void shouldPrintToStdOutAndReturnNull() {
    Object testObject = new Object();

    new EnvironmentBuilder()
      .withStaticVariable("object", testObject)
      .launch(validator -> {
        validatePrinting(validator, "print()", "\n");
        validatePrinting(validator, "print(object)", testObject + "\n");
        validatePrinting(validator, "print(\"Hello\")", "Hello\n");
        validatePrinting(validator, "print(\"Hello\", 25)", "Hello, 25\n");
        validatePrinting(validator, "print(\"Hello\", 25, true)", "Hello, 25, true\n");
      });
  }

  private void validatePrinting(IExpressionResultValidator validator, String expression, String expected) throws Exception {
    PrintStream vanillaOut = System.out;

    try (
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PrintStream printStream = new PrintStream(outputStream);
    ) {
      System.setOut(printStream);
      validator.validate(expression, (Object) null);

      String printed = outputStream.toString(StandardCharsets.UTF_8);

      // NOTE: All calls to validate() create two invocations under the hood, to validate the
      // vanilla- as well as the optimized expression, thus we need to cut off the second half

      int printedLength = printed.length();
      if (printedLength % 2 != 0)
        throw new IllegalStateException("2*n has to be always even");

      assertEquals(expected, printed.substring(0, printedLength / 2));
    } finally {
      System.setOut(vanillaOut);
    }
  }
}
```
</details>


### r_index

Returns the last index of the passed substring within the input string. Returns -1 if the searched
string is not at all present in the input string.

| Argument | Description          |
|----------|----------------------|
| input    | Input to search in   |
| search   | String to search for |

```
r_index(input: String, search: String): Number
```

<details>
<summary>RIndexFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class RIndexFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("r_index()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("r_index(\"\")", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("r_index(null, null)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldReturnFirstIndexOf() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("r_index(\"hello, world\", \"o\")", 8);
        validator.validate("r_index(\"hello, world\", \"l\")", 10);
        validator.validate("r_index(\"hello, world\", \"x\")", -1);
      });
  }
}
```
</details>


### split

Returns a list of resulting substrings based on splitting the input string based on the delimiter.

| Argument  | Description                       |
|-----------|-----------------------------------|
| input     | Input string to split             |
| delimiter | Delimiter to split on, default "," |

```
split(input: String, delimiter?: String): List<String>
```

<details>
<summary>SplitFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class SplitFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("split()", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldSplitOnDefaultString() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("split(\"hello,world,test\")", List.of("hello", "world", "test"));
        validator.validate("split(\"another , weird,ex am ple\")", List.of("another ", " weird", "ex am ple"));
      });
  }

  @Test
  public void shouldSplitOnCustomString() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("split(\"hello|world|test\", \"\\|\")", List.of("hello", "world", "test"));
        validator.validate("split(\"hello|world,test\", \"\\|\")", List.of("hello", "world,test"));
        validator.validate("split(\"another , weird|ex am ple\", \"\\|\")", List.of("another , weird", "ex am ple"));
        validator.validate("split(\"myhelloworldhellotext\", \"hello\")", List.of("my", "world", "text"));
      });
  }
}
```
</details>


### str

Interpret the input value as a string by making use of the environments value interpreter.

| Argument | Description                    |
|----------|--------------------------------|
| input    | Value to interpret as a string |

```
str(input?: Object): String
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/StringFunctionTests.java -->

### substring

Returns a substring of the input, based on the start- and end indices.

| Argument | Description                                                  |
|----------|--------------------------------------------------------------|
| input    | Input string to compute a substring of                       |
| start    | Start index, inclusive, zero-based                           |
| end      | End index, exclusive, zero-based, defaults to input's length |

```
substring(input: String, start: Number, end?: Number): String
```

<details>
<summary>SubstringFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class SubStringFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("substring()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("substring(\"\")", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldThrowOnOutOfRangeIndices() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("substring(\"\", 1)", InvalidFunctionInvocationError.class);
        validator.validateThrows("substring(\"hello\", 1, 20)", InvalidFunctionInvocationError.class);
        validator.validateThrows("substring(\"hello\", -1, 20)", InvalidFunctionInvocationError.class);
        validator.validateThrows("substring(\"hello\", 1, -2)", InvalidFunctionInvocationError.class);
        validator.validateThrows("substring(\"hello\", 4, 2)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldReturnSubstrings() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("substring(\"Hello, world\", 0, 0)", "");
        validator.validate("substring(\"Hello, world\", 5, 5)", "");
        validator.validate("substring(\"Hello, world\", 1, 5)", "ello");
        validator.validate("substring(\"Hello, world\", 2)", "llo, world");
        validator.validate("substring(\"Hello, world\", 0, 8)", "Hello, w");
      });
  }
}
```
</details>


### title_case

Transform the input string to title case (capitalize every word).

| Argument | Description                               |
|----------|-------------------------------------------|
| input    | Input string to transform into title case |

```
title_case(input: String): String
```

<details>
<summary>TitleCaseFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class TitleCaseFunctionTests {

  @Test
  public void shouldRequireInputArgument() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("title_case()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("title_case(null)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldTitleCaseItsInput() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("title_case(\"hello world\")", "Hello World");
        validator.validate("title_case(\"hElLo wOrlD\")", "Hello World");
        validator.validate("title_case(\"hello,world\")", "Hello,World");
        validator.validate("title_case(\"hello_world\")", "Hello_World");
      });
  }
}
```
</details>


### value

Extracts the value from a Java `Map.Entry<?, ?>`.

| Argument | Description           |
|----------|-----------------------|
| entry    | Entry to extract from |

```
value(entry: Map.Entry<?, ?>): Object
```

<details>
<summary>ValueFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class ValueFunctionTests {

  @Test
  public void shouldThrowOnNonMapEntryInput() {
    createEnvironment().launch(validator -> {
      validator.validateThrows("value(my_number, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("value(my_string, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("value(my_boolean, () => \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("value(my_list, () => \"\")", InvalidFunctionArgumentTypeError.class);
    });
  }

  @Test
  public void shouldReturnMapKeys() {
    createEnvironment().launch(validator -> {
      validator.validate("value(list(my_map)[0])", "#FF0000");
      validator.validate("value(list(my_map)[1])", "#00FF00");
      validator.validate("value(list(my_map)[2])", "#0000FF");
      validator.validate("value(null)", (Object) null);
    });
  }

  private EnvironmentBuilder createEnvironment() {
    return new EnvironmentBuilder()
      .withStaticVariable("my_list", createColorList())
      .withStaticVariable("my_list_empty", List.of())
      .withStaticVariable("my_map", createColorMap())
      .withStaticVariable("my_map_empty", Map.of())
      .withStaticVariable("my_number", 1)
      .withStaticVariable("my_string", "hello world")
      .withStaticVariable("my_boolean", true);
  }

  private List<String> createColorList() {
    return List.of("red", "green", "blue");
  }

  private Map<String, String> createColorMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("red", "#FF0000");
    map.put("green", "#00FF00");
    map.put("blue", "#0000FF");
    return map;
  }
}
```
</details>


### range

Returns a list containing all the numbers included in the range.

| Argument | Description            |
|----------|------------------------|
| start    | Start index, inclusive |
| end      | End index, inclusive   |

```
range(start: Number, end: Number): List<Number>
```

<details>
<summary>RangeFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class RangeFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("range()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("range(0)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldReturnEmptyListsOnMalformedRanges() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("range(1, 0)", List.of());
        validator.validate("range(3, -5)", List.of());
      });
  }

  @Test
  public void shouldReturnRangeLists() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("range(0, 0)", List.of(0));
        validator.validate("range(0, 1)", List.of(0, 1));
        validator.validate("range(8, 12)", List.of(8, 9, 10, 11, 12));
        validator.validate("range(-2, 3)", List.of(-2, -1, 0, 1, 2, 3));
        validator.validate("range(-5, -3)", List.of(-5, -4, -3));
      });
  }
}
```
</details>


### flatten

Returns a list containing all parameters provided, where collection items are flattened into the result.

| Argument | Description                     |
|----------|---------------------------------|
| input... | Variable amount of input values |

```
flatten(input...?: Object): List<Object>
```

<details>
<summary>FlattenFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class FlattenFunctionTests {

  @Test
  public void shouldFlattenCollectionsAndOtherValues() {
    new EnvironmentBuilder()
      .withStaticVariable("list_a", List.of(1, 2, 3))
      .withStaticVariable("list_b", List.of(4, 5, 6))
      .withStaticVariable("list_complex", List.of(List.of(7, 8), List.of(9, 10)))
      .launch(validator -> {
        validator.validate("flatten(list_a, list_b, list_complex)", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        validator.validate("flatten(\"Hello\", list_a, list_b, true, list_complex)", List.of("Hello", 1, 2, 3, 4, 5, 6, true, 7, 8, 9, 10));
      });
  }
}
```
</details>


### min

Returns the smaller of two values.

| Argument | Description |
|----------|-------------|
| a        | Value A     |
| b        | Value B     |

```
min(a: Object, b: Object): Object
```

<details>
<summary>MinFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class MinFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("min()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("min(0)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldReturnTheSmallerValue() {
    new EnvironmentBuilder()
      .withStaticVariable("my_list", List.of(1))
      .withStaticVariable("my_list_empty", List.of())
      .launch(validator -> {
        validator.validate("min(0, 5)", 0);
        validator.validate("min(-3, -8)", -8);
        validator.validate("min(my_list, my_list_empty)", List.of());
      });
  }
}
```
</details>


### max

Returns the bigger of two values.

| Argument | Description |
|----------|-------------|
| a        | Value A     |
| b        | Value B     |

```
max(a: Object, b: Object): Object
```

<details>
<summary>MaxFunctionTests.java</summary>

```java
package me.blvckbytes.gpeee.std;

public class MaxFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("max()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("max(0)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldReturnTheBiggerValue() {
    new EnvironmentBuilder()
      .withStaticVariable("my_list", List.of(1))
      .withStaticVariable("my_list_empty", List.of())
      .launch(validator -> {
        validator.validate("max(0, 5)", 5);
        validator.validate("max(-3, -8)", -3);
        validator.validate("max(my_list, my_list_empty)", List.of(1));
      });
  }
}
```
</details>


