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
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class FlattenFunctionTests {

  @Test
  public void shouldFlattenCollectionsAndOtherValues() {
    new EnvironmentBuilder()
      .withStaticVariable("list_a", Arrays.asList(1, 2, 3))
      .withStaticVariable("list_b", Arrays.asList(4, 5, 6))
      .withStaticVariable("list_complex", Arrays.asList(Arrays.asList(7, 8), Arrays.asList(9, 10)))
      .launch(validator -> {
        validator.validate("flatten(list_a, list_b, list_complex)", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        validator.validate("flatten(\"Hello\", list_a, list_b, true, list_complex)", Arrays.asList("Hello", 1, 2, 3, 4, 5, 6, true, 7, 8, 9, 10));
      });
  }
}
