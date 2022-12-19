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

import me.blvckbytes.gpeee.error.InvalidFunctionArgumentTypeError;
import me.blvckbytes.gpeee.error.NonNamedFunctionArgumentError;
import me.blvckbytes.gpeee.error.UndefinedFunctionError;
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.FExpressionFunctionBuilder;
import org.junit.Test;

import java.util.*;

public class FunctionCallTests {

  @Test
  public void shouldCallArgLessFunction() {
    new EnvironmentBuilder()
      .withFunction(
        "my_func",
        new FExpressionFunctionBuilder()
          .build((env, args) -> "Hello, world")
      )
      .launch(validator -> {
        validator.validate("my_func()", "Hello, world");
      });
  }

  @Test
  public void shouldCallArgFulFunction() {
    new EnvironmentBuilder()
      .withFunction(
        "my_func",
        new FExpressionFunctionBuilder()
          .withArg("number", "number to append to the hello string", true)
          .build((env, args) -> "hello " + env.getValueInterpreter().asString(args.get(0)))
      )
      .withFunction(
        "my_func2",
        new FExpressionFunctionBuilder()
          .withArg("number", "number to prepend to the callback's result", true, Long.class)
          .withArg("cb", "callback to evaluate", false, AExpressionFunction.class)
          .withArg("number2", "number to append to the callback result", false, Long.class)
          .build((env, args) -> {
            String result = env.getValueInterpreter().asString(args.get(0));

            if (args.get(1) != null)
              result += env.getValueInterpreter().asString(((AExpressionFunction) args.get(1)).apply(env, List.of(result)));

            if (args.get(2) != null)
              result += env.getValueInterpreter().asString(args.get(2));

            return result;
          })
      )
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
        "my_func",
        new FExpressionFunctionBuilder()
          .withArg("a", "Input A", true)
          .withArg("b", "Input B", false)
          .withArg("c", "Input C", false)
          .withArg("d", "Input D", false)
          .build((env, args) -> (
            (args.get(0) == null ? "" : "a") +
            (args.get(1) == null ? "" : "b") +
            (args.get(2) == null ? "" : "c") +
            (args.get(3) == null ? "" : "d")
          ))
      )
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
      });
  }

  @Test
  public void shouldAcceptFunctionArguments() {
    new EnvironmentBuilder()
      .withFunction(
        "add_one",
        new FExpressionFunctionBuilder()
          .withArg("a", "Input A", true, Long.class)
          .build((env, args) -> (((Long) args.get(0)) + 1))
      )
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
        new FExpressionFunctionBuilder()
          .build((env, args) -> {
            long sum = 0;

            for (Object arg : args)
              sum += env.getValueInterpreter().asLong(arg);

            return sum;
          })
      )
      .launch(validator -> {
        validator.validate("sum(1)", 1);
        validator.validate("sum(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)", 55);
      });
  }

  @Test
  public void testIterCatAndKeyValue() {
    new EnvironmentBuilder()
      .withLiveVariable("my_map", () -> {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("red", "#FF0000");
        map.put("green", "#00FF00");
        map.put("blue", "#0000FF");
        return map;
      })
      .launch(validator -> {
        validator.validate(
          "iter_cat(my_map, (it, ind) -> \"(\" & ind & \" -> \" & key(it) & \"-\" & value(it) & \")\", \", \")",
          "(0 -> red-#FF0000), (1 -> green-#00FF00), (2 -> blue-#0000FF)"
        );
      });
  }

  /**
   * Builds an expression function which only returns the input if the first
   * passed argument is an instance of the specified type and which requires
   * it's first argument to be present and of that type on all call sites.
   * @param type Target type
   */
  private AExpressionFunction buildTypeValidatorFunction(Class<?> type) {
    return new FExpressionFunctionBuilder()
      .withArg("a", "Input A", true, type)
      .build((env, args) -> type.isInstance(args.get(0)) ? args.get(0) : "<error>");
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
      });
  }
}
