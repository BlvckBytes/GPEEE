/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
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

package me.blvckbytes.gpeee.std;

import me.blvckbytes.gpeee.EnvironmentBuilder;
import me.blvckbytes.gpeee.error.InvalidFunctionArgumentTypeError;
import me.blvckbytes.gpeee.error.InvalidFunctionInvocationError;
import org.junit.jupiter.api.Test;

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
