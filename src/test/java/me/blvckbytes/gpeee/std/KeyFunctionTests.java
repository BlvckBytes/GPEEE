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
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeyFunctionTests {

  @Test
  public void shouldThrowOnNonMapEntryInput() {
    createEnvironment().launch(validator -> {
      validator.validateThrows("key(my_number, () -> \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("key(my_string, () -> \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("key(my_boolean, () -> \"\")", InvalidFunctionArgumentTypeError.class);
      validator.validateThrows("key(my_list, () -> \"\")", InvalidFunctionArgumentTypeError.class);
    });
  }

  @Test
  public void shouldReturnMapKeys() {
    createEnvironment().launch(validator -> {
      validator.validate("key(list(my_map)[0])", "red");
      validator.validate("key(list(my_map)[1])", "green");
      validator.validate("key(list(my_map)[2])", "blue");
      validator.validate("key(null)", (Object) null);
    });
  }

  private EnvironmentBuilder createEnvironment() {
    return new EnvironmentBuilder()
      .withStaticVariable("my_list", createColorList())
      .withStaticVariable("my_list_empty", List.of())
      .withStaticVariable("my_map", createColorMap())
      .withStaticVariable("my_map_empty", Map.of())
      .withStaticVariable("my_number", 1)
      .withStaticVariable("my_string", "hello world")
      .withStaticVariable("my_boolean", true);
  }

  private List<String> createColorList() {
    return List.of("red", "green", "blue");
  }

  private Map<String, String> createColorMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("red", "#FF0000");
    map.put("green", "#00FF00");
    map.put("blue", "#0000FF");
    return map;
  }
}
