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

import java.util.*;

public class ListFunctionTests {

  @Test
  public void shouldInterpretValuesAsAList() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_list", Collections.singletonList(1))
      .withStaticVariable("my_list_empty", Collections.emptyList())
      .withStaticVariable("my_map", Collections.singletonMap("k", "v"))
      .withStaticVariable("my_map_empty", Collections.emptyMap());

    env.launch(validator -> {
      validator.validate("list(0)", Collections.singletonList(0));
      validator.validate("list(1)", Collections.singletonList(1));
      validator.validate("list(100)", Collections.singletonList(100));
      validator.validate("list(-1)", Collections.singletonList(-1));
      validator.validate("list(-100)", Collections.singletonList(-100));

      validator.validate("list(1.1)", Collections.singletonList(1.1));
      validator.validate("list(100.1)", Collections.singletonList(100.1));
      validator.validate("list(-1.1)", Collections.singletonList(-1.1));
      validator.validate("list(-100.1)", Collections.singletonList(-100.1));

      validator.validate("list(\"\")", Collections.singletonList(""));
      validator.validate("list(\"non-empty\")", Collections.singletonList("non-empty"));

      validator.validate("list(null)", Collections.emptyList());
      validator.validate("list(true)", Collections.singletonList(true));
      validator.validate("list(false)", Collections.singletonList(false));

      // Lists should be passed through
      validator.validate("list(my_list)", env.getVariable("my_list"));
      validator.validate("list(my_list_empty)", env.getVariable("my_list_empty"));

      // Maps should be converted to their entry-set
      validator.validate("list(my_map)", ((Map<?, ?>) Objects.requireNonNull(env.getVariable("my_map"))).entrySet());
      validator.validate("list(my_map_empty)", ((Map<?, ?>) Objects.requireNonNull(env.getVariable("my_map_empty"))).entrySet());
    });
  }
}
