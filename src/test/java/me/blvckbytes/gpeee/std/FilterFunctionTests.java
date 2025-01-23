/*
 * MIT License
 *
 * Copyright (c) 2025 BlvckBytes
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class FilterFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .withStaticVariable("items", Collections.emptyList())
      .launch(validator -> {
        validator.validateThrows("filter()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("filter(items)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldFilterInputItems() {
    new EnvironmentBuilder()
      .withStaticVariable("items", Arrays.asList("a", "b", "c", null))
      .launch(validator -> {
        validator.validate("filter(items, (item) => item != \"a\")", Arrays.asList("b", "c", null));
        validator.validate("filter(items, (item) => item != \"c\")", Arrays.asList("a", "b", null));
        validator.validate("filter(items, (item) => item != null)", Arrays.asList("a", "b", "c"));
      });
  }
}
