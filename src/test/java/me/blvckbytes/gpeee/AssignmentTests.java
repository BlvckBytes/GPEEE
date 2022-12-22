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

import me.blvckbytes.gpeee.error.IdentifierInUseError;
import me.blvckbytes.gpeee.error.UnexpectedTokenError;
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AssignmentTests {

  @Test
  public void shouldReturnAssignedValue() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("a = 5", 5);
        validator.validate("a = \"hello world\"", "hello world");
        validator.validate("a = 1.2", 1.2);
        validator.validate("a = true", true);
        validator.validate("a = null", (Object) null);
      });
  }

  @Test
  public void shouldThrowIfIdentifierIsInUse() {
    new EnvironmentBuilder()
      .withStaticVariable("my_static_variable", 5)
      .withLiveVariable("my_live_variable", () -> 10)
      .withFunction("my_function", new AExpressionFunction() {
        @Override
        public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
          return null;
        }

        @Override
        public @Nullable List<ExpressionFunctionArgument> getArguments() {
          return null;
        }
      })
      .launch(validator -> {
        validator.validateThrows("my_static_variable = 5", IdentifierInUseError.class);
        validator.validateThrows("my_live_variable = 5", IdentifierInUseError.class);
        validator.validateThrows("my_function = () => 5", IdentifierInUseError.class);
      });
  }

  @Test
  public void shouldThrowIfMalformed() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("a =", UnexpectedTokenError.class);
      });
  }
}
