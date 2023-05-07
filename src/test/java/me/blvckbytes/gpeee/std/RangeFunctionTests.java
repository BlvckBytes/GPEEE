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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

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
        validator.validate("range(1, 0)", Collections.emptyList());
        validator.validate("range(3, -5)", Collections.emptyList());
      });
  }

  @Test
  public void shouldReturnRangeLists() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("range(0, 0)", Collections.singletonList(0));
        validator.validate("range(0, 1)", Arrays.asList(0, 1));
        validator.validate("range(8, 12)", Arrays.asList(8, 9, 10, 11, 12));
        validator.validate("range(-2, 3)", Arrays.asList(-2, -1, 0, 1, 2, 3));
        validator.validate("range(-5, -3)", Arrays.asList(-5, -4, -3));
      });
  }
}
