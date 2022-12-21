/*
 * MIT License
 *
 * Copyright (c) 2022 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.*;
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.*;

public class FunctionInvocationTests {

  @Test
  public void shouldCallArgLessFunction() {
    new EnvironmentBuilder()
      .withFunction(
        "my_func",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            return "Hello, world";
          }

          @Override
          public @Nullable List<ExpressionFunctionArgument> getArguments() {
            return null;
          }
        })
      .launch(validator -> validator.validate("my_func()", "Hello, world"));
  }

  @Test
  public void shouldCallArgFulFunction() {
    new EnvironmentBuilder()
      .withFunction(
        "my_func",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            return "hello " + environment.getValueInterpreter().asString(nullable(args, 0));
          }

          @Override
          public List<ExpressionFunctionArgument> getArguments() {
            return List.of(
              new ExpressionFunctionArgument("number", "number appended to the hello string", true)
            );
          }
        })
      .withFunction(
        "my_func2",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            String result = environment.getValueInterpreter().asString(nullable(args, 0));

            if (args.get(1) != null) {
              AExpressionFunction callback = nonNull(args, 1);
              result += environment.getValueInterpreter().asString(callback.apply(environment, List.of(result)));
            }

            if (args.get(2) != null)
              result += environment.getValueInterpreter().asString(args.get(2));

            return result;
          }

          @Override
          public List<ExpressionFunctionArgument> getArguments() {
            return List.of(
              new ExpressionFunctionArgument("number", "number to prepend to the callback's result", true, Long.class),
              new ExpressionFunctionArgument("cb", "callback to evaluate", false, AExpressionFunction.class),
              new ExpressionFunctionArgument("number2", "number to append to the callback result", false, Long.class)
            );
          }
        })
      .launch(validator -> {
        validator.validate("my_func(2)", "hello 2");
        validator.validate("my_func(55)", "hello 55");
        validator.validateThrows("my_func()", InvalidFunctionArgumentTypeError.class);

        validator.validate("my_func2(2)", "2");
        validator.validate("my_func2(2, () -> \"callback\")", "2callback");
        validator.validate("my_func2(2, (first) -> first & \"callback\", 3)", "22callback3");
        validator.validate("my_func2(2, cb=() -> \"callback\", number2=3)", "2callback3");
        validator.validate("my_func2(2, number2=3)", "23");
        validator.validateThrows("my_func2()", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldAcceptPositionalArguments() {
    new EnvironmentBuilder()
      .withFunction(
        "my_argless_func",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            return null;
          }

          @Override
          public @Nullable List<ExpressionFunctionArgument> getArguments() {
            return null;
          }
        })
      .withFunction(
        "my_func",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            return (args.get(0) == null ? "" : "a") +
              (args.get(1) == null ? "" : "b") +
              (args.get(2) == null ? "" : "c") +
              (args.get(3) == null ? "" : "d");
          }

          @Override
          public List<ExpressionFunctionArgument> getArguments() {
            return List.of(
              new ExpressionFunctionArgument("a", "Input A", true),
              new ExpressionFunctionArgument("b", "Input B", false),
              new ExpressionFunctionArgument("c", "Input C", false),
              new ExpressionFunctionArgument("d", "Input D", false)
            );
          }
        })
      .launch(validator -> {
        // a is required
        validator.validateThrows("my_func()", InvalidFunctionArgumentTypeError.class);

        validator.validate("my_func(1)", "a");
        validator.validate("my_func(1, b=1)", "ab");
        validator.validate("my_func(1, c=1)", "ac");
        validator.validate("my_func(1, d=1)", "ad");
        validator.validate("my_func(1, b=2, d=1)", "abd");

        // Cannot use non-named arguments after named arguments
        validator.validateThrows("my_func(1, b=2, d=1, 2)", NonNamedFunctionArgumentError.class);

        // Cannot specify a named argument which the function itself doesn't define
        validator.validateThrows("my_func(1, unknown=2)", UndefinedFunctionArgumentNameError.class);
        validator.validateThrows("my_func(1, unknown=2)", UndefinedFunctionArgumentNameError.class);
        validator.validateThrows("my_argless_func(unknown=2)", UndefinedFunctionArgumentNameError.class);
      });
  }

  @Test
  public void shouldAcceptFunctionArguments() {
    new EnvironmentBuilder()
      .withFunction(
        "add_one",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            Long inputA = nonNull(args, 0);
            return inputA + 1;
          }

          @Override
          public List<ExpressionFunctionArgument> getArguments() {
            return List.of(new ExpressionFunctionArgument("a", "Input A", true, Long.class));
          }
        })
      .launch(validator -> {
        validator.validateThrows("add_one()", InvalidFunctionArgumentTypeError.class);
        validator.validate("add_one(add_one(add_one(1)))", 4);
      });
  }

  @Test
  public void shouldAcceptVariadicArguments() {
    new EnvironmentBuilder()
      .withFunction(
        "sum",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            long sum = 0;

            for (Object arg : args)
              sum += environment.getValueInterpreter().asLong(arg);

            return sum;
          }

          @Override
          public @Nullable List<ExpressionFunctionArgument> getArguments() {
            return null;
          }
        })
      .launch(validator -> {
        validator.validate("sum(1)", 1);
        validator.validate("sum(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)", 55);
      });
  }

  /**
   * Builds an expression function which only returns the input if the first
   * passed argument is an instance of the specified type and which requires
   * it's first argument to be present and of that type on all call sites.
   * @param type Target type
   */
  private AExpressionFunction buildTypeValidatorFunction(Class<?> type) {
    return new AExpressionFunction() {
      @Override
      public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
        return type.isInstance(args.get(0)) ? args.get(0) : "<error>";
      }

      @Override
      public List<ExpressionFunctionArgument> getArguments() {
        return List.of(new ExpressionFunctionArgument("a", "Input A", true, type));
      }
    };
  }

  @Test
  public void shouldThrowOnTypeMismatch() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withLiveVariable("my_map", HashMap::new)
      .withFunction("my_func", buildTypeValidatorFunction(Map.class));

    env.launch(validator -> {
      validator.validateThrows("my_func()", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("my_func(5)", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("my_func(\"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("my_func(2.2)", InvalidFunctionArgumentTypeError.class);
      validator.validate("my_func(my_map)", env.getVariable("my_map"));
    });
  }

  @Test
  public void shouldThrowOnMalformedSyntax() {
    new EnvironmentBuilder()
      .withFunction(
        "my_func",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            return null;
          }

          @Override
          public List<ExpressionFunctionArgument> getArguments() {
            return List.of(new ExpressionFunctionArgument("a", "test parameter", false));
          }
        })
      .launch(validator -> {
        // Needs to end with a closing parenthesis
        validator.validateThrows("my_func(", UnexpectedTokenError.class);

        // Expected an identifier as the argument
        validator.validateThrows("my_func(,)", UnexpectedTokenError.class);

        // Expected value for a
        validator.validateThrows("my_func(a=,)", UnexpectedTokenError.class);
      });
  }

  @Test
  public void shouldAutoConvertArgs() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_map", Map.of("k1", "v1", "k2", "v2"))
      .withStaticVariable("my_list", List.of("v1", "v2", "v3"))
      .withStaticVariable("my_map_empty", Map.of())
      .withStaticVariable("my_list_empty", List.of())
      .withFunction("list_func", buildTypeValidatorFunction(Collection.class))
      .withFunction("string_func", buildTypeValidatorFunction(String.class))
      .withFunction("long_func", buildTypeValidatorFunction(Long.class))
      .withFunction("double_func", buildTypeValidatorFunction(Double.class))
      .withFunction("boolean_func", buildTypeValidatorFunction(Boolean.class));

    env.launch(validator -> {
      // Collections can only accept other collections or a map (which will be transformed to a EntrySet-list)
      validator.validateThrows("list_func(5)", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("list_func(5.5)", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("list_func(\"test\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("list_func(false)", InvalidFunctionArgumentTypeError.class);
      validator.validate("list_func(my_list)", env.getVariable("my_list"));

      // Stringify result for ease of comparison
      validator.validate("str(list_func(my_map))", env.stringifiedPermutations("my_map"));

      // Everything should be stringified
      validator.validate("string_func(5)", "5");
      validator.validate("string_func(5.5)", "5.5");
      validator.validate("string_func(my_list)", env.stringify(env.getVariable("my_list")));
      validator.validate("string_func(my_map)", env.stringifiedPermutations("my_map"));
      validator.validate("string_func(false)", "false");
      validator.validate("string_func(\"test\")", "test");

      // Numbers aren't parsed from strings by default - that's intended, use std-functions to parse numbers.

      // Everything should be interpretable as a long
      validator.validate("long_func(5)", 5);
      validator.validate("long_func(\"5\")", 1);
      validator.validate("long_func(3.3)", 3);
      validator.validate("long_func(\"3.3\")", 1);
      validator.validate("long_func(-3.3)", -3);
      validator.validate("long_func(\"\")", 0);
      validator.validate("long_func(\"test\")", 1);
      validator.validate("long_func(false)", 0);
      validator.validate("long_func(true)", 1);
      validator.validate("long_func(my_list)", 1);
      validator.validate("long_func(my_map)", 1);
      validator.validate("long_func(my_list_empty)", 0);
      validator.validate("long_func(my_map_empty)", 0);

      // Everything should be interpretable as a double
      validator.validate("double_func(5)", 5.0);
      validator.validate("double_func(\"5\")", 1.0);
      validator.validate("double_func(3.3)", 3.3);
      validator.validate("double_func(\"3.3\")", 1.0);
      validator.validate("double_func(-3.3)", -3.3);
      validator.validate("double_func(\"\")", 0.0);
      validator.validate("double_func(\"test\")", 1.0);
      validator.validate("double_func(false)", 0.0);
      validator.validate("double_func(true)", 1.0);
      validator.validate("double_func(my_list)", 1.0);
      validator.validate("double_func(my_map)", 1.0);
      validator.validate("double_func(my_list_empty)", 0.0);
      validator.validate("double_func(my_map_empty)", 0.0);

      // Everything should be interpretable as a boolean
      validator.validate("boolean_func(5)", true);
      validator.validate("boolean_func(\"5\")", true);
      validator.validate("boolean_func(3.3)", true);
      validator.validate("boolean_func(\"3.3\")", true);
      validator.validate("boolean_func(-3.3)", false);
      validator.validate("boolean_func(\"\")", false);
      validator.validate("boolean_func(\"test\")", true);
      validator.validate("boolean_func(false)", false);
      validator.validate("boolean_func(true)", true);
      validator.validate("boolean_func(my_list)", true);
      validator.validate("boolean_func(my_map)", true);
      validator.validate("boolean_func(my_list_empty)", false);
      validator.validate("boolean_func(my_map_empty)", false);
    });
  }

  @Test
  public void shouldNotCallAnUndefinedFunction() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("unknown_func()", UndefinedFunctionError.class);
        validator.validate("unknown_func?()", (Object) null);
      });
  }

  @Test
  public void shouldInvertOrNegateFunctionReturns() {
    new EnvironmentBuilder()
      .withFunction(
        "get_my_number",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            return 5;
          }

          @Override
          public @Nullable List<ExpressionFunctionArgument> getArguments() {
            return null;
          }
        })
      .withFunction(
        "get_my_boolean",
        new AExpressionFunction() {
          @Override
          public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
            return true;
          }

          @Override
          public @Nullable List<ExpressionFunctionArgument> getArguments() {
            return null;
          }
        })
      .launch(validator -> {
        // Invert sign of function return
        validator.validate("get_my_number()", 5);
        validator.validate("-get_my_number()", -5);

        // Flip function return boolean
        validator.validate("get_my_boolean()", true);
        validator.validate("not get_my_boolean()", false);
      });
  }
}
