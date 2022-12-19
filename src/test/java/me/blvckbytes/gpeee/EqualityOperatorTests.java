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
  public void shouldEqualExactLongDouble() {
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
}
