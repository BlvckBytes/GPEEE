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
import me.blvckbytes.gpeee.functions.FExpressionFunctionBuilder;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

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
      .launch(validator -> {
        validator.validate("my_func(2)", "hello 2");
        validator.validate("my_func(55)", "hello 55");
        validator.validateThrows("my_func()", InvalidFunctionArgumentTypeError.class);
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
}
