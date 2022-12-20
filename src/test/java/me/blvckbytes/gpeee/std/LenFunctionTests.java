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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class LenFunctionTests {

  @Test
  public void shouldRespondWithAnItemsLength() {
    new EnvironmentBuilder()
      .withStaticVariable("my_list", List.of(1, 2, 3, 4))
      .withStaticVariable("my_list_empty", List.of())
      .withStaticVariable("my_map", Map.of("k", "v", "k2", "v2"))
      .withStaticVariable("my_map_empty", Map.of())
      .withStaticVariable("my_array", new int[] { 1, 2, 3 })
      .withStaticVariable("my_array_empty", new int[] {})
      .withStaticVariable("my_string", "hello, world")
      .launch(validator -> {
        validator.validate("len(my_list)", 4);
        validator.validate("len(my_list_empty)", 0);
        validator.validate("len(my_map)", 2);
        validator.validate("len(my_map_empty)", 0);
        validator.validate("len(my_array)", 3);
        validator.validate("len(my_array_empty)", 0);
        validator.validate("len(my_string)", "hello, world".length());
        validator.validate("len(null)", 0);
      });
  }
}
