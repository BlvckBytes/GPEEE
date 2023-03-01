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

While this implementation is a standard function, the exact same way of implementing functions applies to custom functions, 
minus the registration- as well as the result return boolean parts.

### Full Use Example

The following class shows a compact but complete use-case of the `GPEEE`.

<!-- #include src/test/java/me/blvckbytes/gpeee/FullUseExample.java -->

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

<!-- #include src/main/resources/grammar.ebnf -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/BoolFunctionTests.java -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/DateFormatFunctionTests.java -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/IterCatFunctionTests.java -->

### key

Extracts the key from a Java `Map.Entry<?, ?>`.

| Argument | Description           |
|----------|-----------------------|
| entry    | Entry to extract from |

```
key(entry: Map.Entry<?, ?>): Object
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/KeyFunctionTests.java -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/LenFunctionTests.java -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/LIndexFunctionTests.java -->

### list

Interpret the input value as a list. Scalar values will create singleton lists, lists will be passed through and
maps will be converted to lists of their entry-sets.

| Argument | Description                  |
|----------|------------------------------|
| input    | Input to interpret as a list |

```
list(input?: Object): List<?>
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/ListFunctionTests.java -->

### list_of

Create a list from a variable amount of scalar input values.

| Argument | Description                     |
|----------|---------------------------------|
| input... | Variable amount of input values |

```
list_of(value...?: Object): List<?>
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/ListOfFunctionTests.java -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/MapFunctionTests.java -->

### map_of

Create a list from a variable amount of scalar input value pairs.

| Argument | Description                                     |
|----------|-------------------------------------------------|
| input... | Variable amount of input values, taken in pairs |

```
map_of(value...?: Object): Map<?, ?>
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/MapOfFunctionTests.java -->

### print

Print the input values to STDOUT.

| Argument | Description                     |
|----------|---------------------------------|
| input... | Variable amount of input values |

```
print(input...?: Object): void
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/PrintFunctionTests.java -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/RIndexFunctionTests.java -->

### split

Returns a list of resulting substrings based on splitting the input string based on the delimiter.

| Argument  | Description                       |
|-----------|-----------------------------------|
| input     | Input string to split             |
| delimiter | Delimiter to split on, default "," |

```
split(input: String, delimiter?: String): List<String>
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/SplitFunctionTests.java -->

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

<!-- #include src/test/java/me/blvckbytes/gpeee/std/SubstringFunctionTests.java -->

### title_case

Transform the input string to title case (capitalize every word).

| Argument | Description                               |
|----------|-------------------------------------------|
| input    | Input string to transform into title case |

```
title_case(input: String): String
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/TitleCaseFunctionTests.java -->

### value

Extracts the value from a Java `Map.Entry<?, ?>`.

| Argument | Description           |
|----------|-----------------------|
| entry    | Entry to extract from |

```
value(entry: Map.Entry<?, ?>): Object
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/ValueFunctionTests.java -->

### range

Returns a list containing all the numbers included in the range.

| Argument | Description            |
|----------|------------------------|
| start    | Start index, inclusive |
| end      | End index, inclusive   |

```
range(start: Number, end: Number): List<Number>
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/RangeFunctionTests.java -->

### flatten

Returns a list containing all parameters provided, where collection items are flattened into the result.

| Argument | Description                     |
|----------|---------------------------------|
| input... | Variable amount of input values |

```
flatten(input...?: Object): List<Object>
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/FlattenFunctionTests.java -->

### min

Returns the smaller of two values.

| Argument | Description |
|----------|-------------|
| a        | Value A     |
| b        | Value B     |

```
min(a: Object, b: Object): Object
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/MinFunctionTests.java -->

### max

Returns the bigger of two values.

| Argument | Description |
|----------|-------------|
| a        | Value A     |
| b        | Value B     |

```
max(a: Object, b: Object): Object
```

<!-- #include src/test/java/me/blvckbytes/gpeee/std/MaxFunctionTests.java -->

<!-- #configure include SKIP_LEADING_COMMENTS true -->
<!-- #configure include SKIP_LEADING_EMPTY true -->
<!-- #configure include SKIP_LEADING_PACKAGE false -->
<!-- #configure include SKIP_LEADING_IMPORTS true -->
<!-- #configure include WRAP_IN_COLLAPSIBLE true -->
