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

public class StrFunctionTests {

  @Test
  public void shouldStringifyValues() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_list", Arrays.asList(1, 2, 3))
      .withStaticVariable("my_set", new HashSet<>(Arrays.asList(4, 5, 6)))
      .withStaticVariable("my_array", new Integer[] {7, 8, 9})
      .withStaticVariable("my_map", new HashMap<Object, Object>() {{
        put("One", 1);
        put("Two", 2);
        put("Three", 3);
      }});

    env.launch(validator -> {
      // Long
      validator.validate("str(55)", "55");

      // Double
      validator.validate("str(-.2)", "-0.2");

      // String
      validator.validate("\"already a string\"", "already a string");

      // Literal
      validator.validate("str(true)", "true");
      validator.validate("str(false)", "false");
      validator.validate("str(null)", "<null>");

      // Collections
      validator.validate("str(my_list)", env.stringify(env.getVariable("my_list")));
      validator.validate("str(my_set)", env.stringifiedPermutations("my_set"));
      validator.validate("str(my_array)", env.stringify(env.getVariable("my_array")));

      // Maps
      validator.validate("str(my_map)", env.stringifiedPermutations("my_map"));
    });
  }
}
