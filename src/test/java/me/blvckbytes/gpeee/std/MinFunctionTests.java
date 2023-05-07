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

package me.blvckbytes.gpeee.std;

import me.blvckbytes.gpeee.EnvironmentBuilder;
import me.blvckbytes.gpeee.error.InvalidFunctionArgumentTypeError;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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
      .withStaticVariable("my_list", Collections.singletonList(1))
      .withStaticVariable("my_list_empty", Collections.emptyList())
      .launch(validator -> {
        validator.validate("min(0, 5)", 0);
        validator.validate("min(-3, -8)", -8);
        validator.validate("min(my_list, my_list_empty)", Collections.emptyList());
      });
  }
}
