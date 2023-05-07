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

import java.util.Collections;
import java.util.Map;

public class BoolFunctionTests {

  @Test
  public void shouldInterpretValuesAsABoolean() {
    new EnvironmentBuilder()
      .withStaticVariable("my_list", Collections.singletonList(1))
      .withStaticVariable("my_list_empty", Collections.emptyList())
      .withStaticVariable("my_map", Collections.singletonMap("k", "v"))
      .withStaticVariable("my_map_empty", Collections.emptyMap())
      .launch(validator -> {
        validator.validate("bool(0)", false);
        validator.validate("bool(1)", true);
        validator.validate("bool(100)", true);
        validator.validate("bool(-1)", false);
        validator.validate("bool(-100)", false);

        validator.validate("bool(1.1)", true);
        validator.validate("bool(100.1)", true);
        validator.validate("bool(-1.1)", false);
        validator.validate("bool(-100.1)", false);

        validator.validate("bool(\"\")", false);
        validator.validate("bool(\"non-empty\")", true);

        validator.validate("bool(null)", false);
        validator.validate("bool(true)", true);
        validator.validate("bool(false)", false);

        validator.validate("bool(my_list)", true);
        validator.validate("bool(my_map)", true);
        validator.validate("bool(my_list_empty)", false);
        validator.validate("bool(my_map_empty)", false);
      });
  }
}
