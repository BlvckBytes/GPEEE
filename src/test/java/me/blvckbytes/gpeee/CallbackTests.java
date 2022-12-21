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

public class CallbackTests {

  @Test
  public void shouldParseCallback() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withFunction("my_func", createFunction());

    env.launch(validator -> {
      // Should be able to access as many parameters as needed
      validator.validate("my_func(() => \"ok\")", "ok");
      validator.validate("my_func((a) => \"ok\" & a)", "ok1");
      validator.validate("my_func((a, b) => \"ok\" & a & b)", "ok12");
      validator.validate("my_func((a, b, c) => \"ok\" & a & b & c)", "ok123");

      // Where non-provided callback input parameters will default to null
      validator.validate("my_func((a, b, c, d) => \"ok\" & a & b & c & d)", "ok123" + env.stringify(null));
    });
  }

  @Test
  public void shouldThrowWhenMalformed() {
    new EnvironmentBuilder()
      .withFunction("my_func", createFunction())
      .launch(validator -> {
        validator.validateThrows("my_func((a => \"ok\")", UnexpectedTokenError.class);
        validator.validateThrows("my_func((a) \"ok\")", UnexpectedTokenError.class);
        validator.validateThrows("my_func((a,) => \"ok\")", UnexpectedTokenError.class);
        validator.validateThrows("my_func((a 5) => \"ok\")", UnexpectedTokenError.class);
        validator.validateThrows("my_func((a - 5) => \"ok\")", UnexpectedTokenError.class);
        validator.validateThrows("my_func((a=5) => \"ok\")", UnexpectedTokenError.class);
      });
  }

  private AExpressionFunction createFunction() {
    return new AExpressionFunction() {
      @Override
      public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
        AExpressionFunction func = nonNull(args, 0);
        return func.apply(environment, List.of(1, 2, 3));
      }

      @Override
      public List<ExpressionFunctionArgument> getArguments() {
        return List.of(
          new ExpressionFunctionArgument("cb", "callback expression", true, AExpressionFunction.class)
        );
      }
    };
  }
}
