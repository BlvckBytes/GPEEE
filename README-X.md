<!-- This file is rendered by https://github.com/BlvckBytes/readme_helper -->

# GPEEE

![build](https://github.com/BlvckBytes/GPEEE/actions/workflows/build.yml/badge.svg)
[![coverage](https://codecov.io/gh/BlvckBytes/GPEEE/branch/main/graph/badge.svg?token=WGWX8IDT00)](https://codecov.io/gh/BlvckBytes/GPEEE)

![logo](readme_images/logo.png)

The opensource `General Purpose Environment Expression Evaluator` (GPEEE) which you most definitely
want to integrate into your next project.

<!-- #toc -->

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

### Evaluator Instance

The working principle of this evaluator is as simple as it gets: You provide an *evaluation environment*
as well as a string containing the target expression and the evaluator returns an *evaluation result* or
throws an error to be properly handled by the caller.

<!-- #include src/main/java/me/blvckbytes/gpeee/IExpressionEvaluator.java -->

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

<!-- #include src/main/java/me/blvckbytes/gpeee/interpreter/IEvaluationEnvironment.java -->

If a value is going to be constant throughout the lifetime of an environment, you may use a static variable. Otherwise,
it's advised to not update the map over and over again, but to rather specify a live variable supplier. This supplier
will be called whenever the interpreter needs this variable's value.

The *value interpreter* is used to define how different data-types can be interpreted and combined using various
operations. Implementing such an instance can take a lot of time and is prone to errors, which is why there's a very
sensible default implementation which' use is recommended, as described in the comment inside the above interface.

In order to create a new expression function, it's best practice to create a separate class which inherits the
following abstract base class:

<!-- #include src/main/java/me/blvckbytes/gpeee/functions/AExpressionFunction.java -->

If argument validation is not required, the argument list getter can always just return null. Otherwise, arguments
may be specified in order, where non-required (non-positional) arguments may only follow after all required arguments. An
argument consists of it's name, description, required-flag as well as an optional list of accepted types. Leave this
argument empty to not type-check at all.

<!-- #include src/main/java/me/blvckbytes/gpeee/functions/AExpressionFunction.java -->

This class will also itself try to convert passed values to the required type before letting the interpreter throw a
mismatch error by making use of the *value interpreter*.

### Function Implementation Example

There are a few standard (std) functions already included in this software package you can take a look at in order
to get a feel for how functions are intended to be used and created. As an example, an excerpt from the std function
`iter_cat` has been provided for you.

<!-- #include src/main/java/me/blvckbytes/gpeee/functions/std/IterCatFunction.java -->

### Full Use Example

The following class shows a compact but complete use-case of the `GPEEE`.

<!-- #include src/test/java/me/blvckbytes/gpeee/FullUseExample.java -->

## Syntax

The syntax of this language has been mostly inspired by *JavaScript* as well as *Python* and is easy to grow accustomed
to once you've spent a few minutes to experiment with it.

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

<!-- #include src/main/resources/grammar.ebnf -->

<!-- #configure include SKIP_LEADING_COMMENTS true -->
<!-- #configure include SKIP_LEADING_EMPTY true -->
<!-- #configure include SKIP_LEADING_PACKAGE false -->
<!-- #configure include SKIP_LEADING_IMPORTS true -->
<!-- #configure include WRAP_IN_COLLAPSIBLE true -->
