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
      .withStaticVariable("my_list", createList(1))
      .withStaticVariable("my_list_empty", createList())
      .withStaticVariable("my_map", Map.of("k", "v"))
      .withStaticVariable("my_map_empty", Map.of());

    env.launch(validator -> {
      validator.validate("list(0)", createList(0));
      validator.validate("list(1)", createList(1));
      validator.validate("list(100)", createList(100));
      validator.validate("list(-1)", createList(-1));
      validator.validate("list(-100)", createList(-100));

      validator.validate("list(1.1)", createList(1.1));
      validator.validate("list(100.1)", createList(100.1));
      validator.validate("list(-1.1)", createList(-1.1));
      validator.validate("list(-100.1)", createList(-100.1));

      validator.validate("list(\"\")", createList(""));
      validator.validate("list(\"non-empty\")", createList("non-empty"));

      validator.validate("list(null)", createList());
      validator.validate("list(true)", createList(true));
      validator.validate("list(false)", createList(false));

      // Lists should be passed through
      validator.validate("list(my_list)", env.getVariable("my_list"));
      validator.validate("list(my_list_empty)", env.getVariable("my_list_empty"));

      // Maps should be converted to their entry-set
      validator.validate("list(my_map)", ((Map<?, ?>) Objects.requireNonNull(env.getVariable("my_map"))).entrySet());
      validator.validate("list(my_map_empty)", ((Map<?, ?>) Objects.requireNonNull(env.getVariable("my_map_empty"))).entrySet());
    });
  }

  private List<Object> createList(Object... items) {
    return new ArrayList<>(Arrays.asList(items));
  }
}
