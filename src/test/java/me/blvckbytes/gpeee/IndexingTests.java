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

package me.blvckbytes.gpeee;

import me.blvckbytes.gpeee.error.NonIndexableValueError;
import me.blvckbytes.gpeee.error.UnexpectedTokenError;
import me.blvckbytes.gpeee.functions.FExpressionFunctionBuilder;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class IndexingTests {

  @Test
  public void shouldIndexLists() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_list", List.of(1, 2, 3));

    env.launch(validator -> {
      validator.validate("my_list[0]", 1);
      validator.validate("my_list[1]", 2);
      validator.validate("my_list[2]", 3);
    });
  }

  @Test
  public void shouldIndexArrays() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_array", new Integer[] {7, 8, 9});

    env.launch(validator -> {
      validator.validate("my_array[0]", 7);
      validator.validate("my_array[1]", 8);
      validator.validate("my_array[2]", 9);
    });
  }

  @Test
  public void shouldIndexMaps() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("my_map", Map.of(
        "One", 1,
        "Two", 2,
        "Three", 3,
        "AnotherMap", Map.of(
          "One", 1,
          "Two", 2,
          "Three", 3
        )
      ));

    env.launch(validator -> {
      validator.validate("my_map[\"One\"]", 1);
      validator.validate("my_map[\"Two\"]", 2);
      validator.validate("my_map[\"Three\"]", 3);
      validator.validate("my_map[\"AnotherMap\"][\"One\"]", 1);
      validator.validate("my_map[\"AnotherMap\"][\"Two\"]", 2);
      validator.validate("my_map[\"AnotherMap\"][\"Three\"]", 3);
    });
  }

  @Test
  public void shouldIndexFunctionResult() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withFunction("get_my_map", new FExpressionFunctionBuilder()
        .build((e, args) -> (
          Map.of(
            "One", 1,
            "Two", 2,
            "Three", 3
          )
        )));

    env.launch(validator -> {
      validator.validate("get_my_map()[\"One\"]", 1);
      validator.validate("get_my_map()[\"Two\"]", 2);
      validator.validate("get_my_map()[\"Three\"]", 3);
    });
  }

  @Test
  public void shouldNotIndexNonIndexableValues() {
    new EnvironmentBuilder()
      .withStaticVariable("my_number", 5)
      .withStaticVariable("my_string", "hello world")
      .withStaticVariable("my_null", null)
      .launch(validator -> {
        validator.validateThrows("my_number[\"unknown\"]", NonIndexableValueError.class);
        validator.validateThrows("my_string[1]", NonIndexableValueError.class);
        validator.validateThrows("my_number[\"unknown\"][2]", NonIndexableValueError.class);
        validator.validateThrows("my_string[1][\"unknown\"]", NonIndexableValueError.class);
        validator.validateThrows("my_null[\"unknown\"]", NonIndexableValueError.class);
        validator.validateThrows("my_null[1]", NonIndexableValueError.class);
        validator.validateThrows("my_null[\"unknown\"][2]", NonIndexableValueError.class);
        validator.validateThrows("my_null[1][\"unknown\"]", NonIndexableValueError.class);
      });
  }

  @Test
  public void shouldThrowIfMalformed() {
    new EnvironmentBuilder()
      .withStaticVariable("my_map", Map.of())
      .launch(validator -> {
        // Expected terminator token ]
        validator.validateThrows("my_map[\"test\"", UnexpectedTokenError.class);
      });
  }
}
