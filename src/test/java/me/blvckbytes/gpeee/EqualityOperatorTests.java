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

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EqualityOperatorTests {

  @Test
  public void shouldEqualExactLongLong() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5 === 3", false);
        validator.validate("2 === 2", true);
        validator.validate("8 === 12", false);
        validator.validate("7 === 7", true);

        validator.validate("5 !== 3", true);
        validator.validate("2 !== 2", false);
        validator.validate("8 !== 12", true);
        validator.validate("7 !== 7", false);
      });
  }

  @Test
  public void shouldEqualLongStringLong() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5 == \"3\"", false);
        validator.validate("2 == \"2\"", true);
        validator.validate("8 == \"12\"", false);
        validator.validate("7 == \"7\"", true);

        validator.validate("5 != \"3\"", true);
        validator.validate("2 != \"2\"", false);
        validator.validate("8 != \"12\"", true);
        validator.validate("7 != \"7\"", false);
      });
  }

  @Test
  public void shouldEqualExactDoubleDouble() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5.2 === 3.4", false);
        validator.validate("2.1 === 2.1", true);
        validator.validate("8.3 === 12.5", false);
        validator.validate("7.3 === 7.3", true);

        validator.validate("5.2 !== 3.4", true);
        validator.validate("2.1 !== 2.1", false);
        validator.validate("8.3 !== 12.5", true);
        validator.validate("7.3 !== 7.3", false);
      });
  }

  @Test
  public void shouldNotEqualExactLongDouble() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5 !== 5.0", true);
        validator.validate("2 !== 2.0", true);
        validator.validate("8 !== 8.0", true);
        validator.validate("7 !== 7.0", true);
      });
  }

  @Test
  public void shouldEqualLongStringDouble() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5.2 == \"3.4\"", false);
        validator.validate("2.1 == \"2.1\"", true);
        validator.validate("8.3 == \"12.5\"", false);
        validator.validate("7.3 == \"7.3\"", true);

        // Should ignore unnecessary decimal points
        validator.validate("7.0 == \"7\"", true);
        validator.validate("7.0 == \"7.2\"", false);
        validator.validate("7.2 == \"7\"", false);

        // But should allow for checking against them, even if unnecessary
        validator.validate("7 == \"7.0\"", true);
        validator.validate("7.2 == \"7.0\"", false);
        validator.validate("7 == \"7.2\"", false);

        // Malformed numbers should never equal
        validator.validate("7 == \"7.abc\"", false);

        validator.validate("5.2 != \"3.4\"", true);
        validator.validate("2.1 != \"2.1\"", false);
        validator.validate("8.3 != \"12.5\"", true);
        validator.validate("7.3 != \"7.3\"", false);

        // Just for good measure - try the last block the other way around
        validator.validate("\"3.4\" != 5.2", true);
        validator.validate("\"2.1\" != 2.1", false);
        validator.validate("\"12.5\" != 8.3", true);
        validator.validate("\"7.3\" != 7.3", false);
      });
  }

  @Test
  public void shouldEqualStringString() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("\"hello world\" == \"hello world\"", true);
        validator.validate("\"Hello World\" == \"hellO world\"", true);
        validator.validate("\" Hello World  \" == \"      hellO world\"", true);
        validator.validate("\" Hello other World  \" == \"      hellO world\"", false);

        validator.validate("\"hello world\" != \"hello world\"", false);
        validator.validate("\"Hello World\" != \"hellO world\"", false);
        validator.validate("\" Hello World  \" != \"      hellO world\"", false);
        validator.validate("\" Hello other World  \" != \"      hellO world\"", true);
      });
  }

  @Test
  public void shouldEqualLongCollectionAndMap() {
    new EnvironmentBuilder()
      .withStaticVariable("my_map", Map.of("k", "v"))
      .withStaticVariable("my_list", List.of(1))
      .withStaticVariable("my_empty_map", Map.of())
      .withStaticVariable("my_empty_list", List.of())
      .launch(validator -> {
        // Maps and Collections should have their equivalent long value
        validator.validate("my_map == 1", true);
        validator.validate("my_list == 1", true);
        validator.validate("my_empty_map == 0", true);
        validator.validate("my_empty_list == 0", true);

        // But not when using strict mode
        validator.validate("my_map === 1", false);
        validator.validate("my_list === 1", false);
        validator.validate("my_empty_map === 0", false);
        validator.validate("my_empty_list === 0", false);
      });
  }

  @Test
  public void shouldEqualNullLongDouble() {
    new EnvironmentBuilder()
      .launch(validator -> {
        // Null and zero are not exact equal
        validator.validate("0 === null", false);
        validator.validate("0.0 === null", false);

        // But equal in their numeric value
        validator.validate("0 == null", true);
        validator.validate("0.0 == null", true);

        // Evaluate the numeric value of null as a double by
        // joining it up with a double using a math operator
        validator.validate("0.1 == .1 + null", true);

        // Just to make sure, :^)
        validator.validate("null == null", true);
        validator.validate("null === null", true);

      });
  }

  @Test
  public void shouldEqualExactStringString() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("\"hello world\" === \"hello world\"", true);
        validator.validate("\"Hello World\" === \"hello world\"", false);
        validator.validate("\"hello other world\" === \"hello world\"", false);

        validator.validate("\"hello world\" !== \"hello world\"", false);
        validator.validate("\"Hello World\" !== \"hello world\"", true);
        validator.validate("\"hello other world\" !== \"hello world\"", true);
      });
  }

  @Test
  public void shouldEqualListListContents() {
    new EnvironmentBuilder()
      .withStaticVariable("my_list_a", List.of(1, 2, 3, 4, 5))
      .withStaticVariable("my_list_b", List.of(1, 2, 3, 4, 5))
      .withStaticVariable("my_list_b_2", List.of(5, 4, 3, 2, 1))
      .withStaticVariable("my_list_empty", List.of())
      .launch(validator -> {
        // List contents equal, so no matter if exact or not, the lists equal
        validator.validate("my_list_a == my_list_b", true);
        validator.validate("my_list_b == my_list_a", true);
        validator.validate("my_list_a === my_list_b", true);
        validator.validate("my_list_b === my_list_a", true);

        // List content differs (one is reversed)

        // Exact will not match
        validator.validate("my_list_a === my_list_b_2", false);
        validator.validate("my_list_b_2 === my_list_a", false);

        // But non-exact will only check for same size and items
        validator.validate("my_list_a == my_list_b_2", true);
        validator.validate("my_list_b_2 == my_list_a", true);

        // Length differs
        validator.validate("my_list_a == my_list_empty", false);
        validator.validate("my_list_a === my_list_empty", false);
      });
  }

  @Test
  public void shouldEqualArrayArrayContents() {
    new EnvironmentBuilder()
      .withStaticVariable("my_array_a", new int[] { 1, 2, 3, 4, 5 })
      .withStaticVariable("my_array_b", new int[] { 1, 2, 3, 4, 5 })
      .withStaticVariable("my_array_b_2", new int[] { 5, 4, 3, 2, 1 })
      .withStaticVariable("my_array_empty", new int[] {})
      .launch(validator -> {
        // Array contents equal, so no matter if exact or not, the maps equal
        validator.validate("my_array_a == my_array_b", true);
        validator.validate("my_array_b == my_array_a", true);
        validator.validate("my_array_a === my_array_b", true);
        validator.validate("my_array_b === my_array_a", true);

        // Array content differs (one is reversed)

        // Exact will not match
        validator.validate("my_array_a === my_array_b_2", false);
        validator.validate("my_array_b_2 === my_array_a", false);

        // But non-exact will only check for same size and items
        validator.validate("my_array_a == my_array_b_2", true);
        validator.validate("my_array_b_2 == my_array_a", true);

        // Length differs
        validator.validate("my_array_a == my_array_empty", false);
        validator.validate("my_array_a === my_array_empty", false);
      });
  }

  @Test
  public void shouldEqualMapMapContents() {
    new EnvironmentBuilder()
      .withStaticVariable("my_map_a", generateOrderedMap(false))
      .withStaticVariable("my_map_b", generateOrderedMap(false))
      .withStaticVariable("my_map_b_2", generateOrderedMap(true))
      .withStaticVariable("my_map_empty", Map.of())
      .launch(validator -> {
        // Map contents equal, so no matter if exact or not, the maps equal
        validator.validate("my_map_a == my_map_b", true);
        validator.validate("my_map_b == my_map_a", true);
        validator.validate("my_map_a === my_map_b", true);
        validator.validate("my_map_b === my_map_a", true);

        // Map content differs (one is reversed)

        // Exact will not match
        validator.validate("my_map_a === my_map_b_2", false);
        validator.validate("my_map_b_2 === my_map_a", false);

        // But non-exact will only check for same size and items
        validator.validate("my_map_a == my_map_b_2", true);
        validator.validate("my_map_b_2 == my_map_a", true);

        // Length differs
        validator.validate("my_map_a == my_map_empty", false);
        validator.validate("my_map_a === my_map_empty", false);
      });
  }

  private Map<String, String> generateOrderedMap(boolean reverse) {
    Map<String, String> res = new LinkedHashMap<>();

    int numItems = 5;
    for (int i = 0; i < numItems; i++) {
      int index = reverse ? (numItems - 1 - i) : i;
      res.put("k" + index, "v" + index);
    }

    return res;
  }
}
