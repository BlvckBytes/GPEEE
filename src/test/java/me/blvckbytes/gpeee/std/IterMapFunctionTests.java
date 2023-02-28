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

import java.util.List;

public class IterMapFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .withStaticVariable("items", List.of())
      .launch(validator -> {
        validator.validateThrows("iter_map()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("iter_map(items)", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldReturnFallbackValueWhenEmpty() {
    new EnvironmentBuilder()
      .withStaticVariable("items_empty", List.of())
      .withStaticVariable("items_one", List.of(1))
      .launch(validator -> {
        validator.validate("iter_map(items_empty, (item) => item, \"empty collection\")", List.of("empty collection"));
        validator.validate("iter_map(items_one, (item) => item, \"empty collection\")", List.of(1));
      });
  }

  @Test
  public void shouldMapInputItems() {
    new EnvironmentBuilder()
      .withStaticVariable("items", List.of("a", "b", "c"))
      .launch(validator -> {
        validator.validate("iter_map(items, (item) => item & \" suffix\")", List.of("a suffix", "b suffix", "c suffix"));
        validator.validate("iter_map(items, (item, index) => index & item)", List.of("0a", "1b", "2c"));
      });
  }
}
