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
import me.blvckbytes.gpeee.error.InvalidFunctionInvocationError;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MapOfFunctionTests {

  @Test
  public void shouldCollectArgumentsIntoAMap() {
    EnvironmentBuilder env = new EnvironmentBuilder();

    env.launch(validator -> {
      validator.validate("map_of(\"k\", 1)", Map.of("k", 1));
      validator.validate("map_of(\"k1\", 1.2, \"k2\", -5, \"k3\", \"value 3\")", Map.of("k1", 1.2, "k2", -5, "k3", "value 3"));
      validator.validate("map_of(\"k\", null)", nullMap("k"));
      validator.validate("map_of()", Map.of());

      validator.validateThrows("map_of(\"k\")", InvalidFunctionInvocationError.class);
      validator.validateThrows("map_of(\"k\", 1, \"k2\")", InvalidFunctionInvocationError.class);
    });
  }

  private Map<String, ?> nullMap(String key) {
    Map<String, ?> result = new HashMap<>();
    result.put(key, null);
    return result;
  }
}
