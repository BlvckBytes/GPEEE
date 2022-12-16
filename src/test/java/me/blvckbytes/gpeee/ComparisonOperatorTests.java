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

import org.junit.Test;

public class ComparisonOperatorTests {

  @Test
  public void shouldCompareLongLong() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5 > 3", true);
        validator.validate("2 >= 2", true);
        validator.validate("8 < 12", true);
        validator.validate("7 <= 7", true);

        validator.validate("5 < 3", false);
        validator.validate("2 < 2", false);
        validator.validate("8 >= 12", false);
        validator.validate("7 < 7", false);
      });
  }

  @Test
  public void shouldCompareLongDouble() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5.3 > 3", true);
        validator.validate("2.2 >= 2", true);
        validator.validate("8.9 < 12", true);
        validator.validate("7 <= 7.1", true);

        validator.validate("5.3 < 3", false);
        validator.validate("2.2 < 2", false);
        validator.validate("8.9 >= 12", false);
        validator.validate("7 > 7.1", false);
      });
  }

  @Test
  public void shouldCompareLongString() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("\"non-empty\" > 0", true);
        validator.validate("2 > \"non-empty\"", true);
        validator.validate("\"\" < 1", true);
        validator.validate("1 > \"\"", true);

        validator.validate("\"non-empty\" <= 0", false);
        validator.validate("2 <= \"non-empty\"", false);
        validator.validate("\"\" >= 1", false);
        validator.validate("1 <= \"\"", false);
      });
  }

  @Test
  public void shouldCompareDoubleString() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("\"non-empty\" > .3", true);
        validator.validate("1.1 > \"non-empty\"", true);
        validator.validate("\"\" < .4", true);
        validator.validate(".2 > \"\"", true);

        validator.validate("\"non-empty\" < .3", false);
        validator.validate("1.1 < \"non-empty\"", false);
        validator.validate("\"\" >= .4", false);
        validator.validate(".2 <= \"\"", false);
      });
  }

  @Test
  public void shouldCompareLiteralString() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("\"non-empty\" > \"\"", true);
        validator.validate("\"non-empty\" > null", true);
        validator.validate("\"non-empty\" <= true", true);
        validator.validate("false < \"non-empty\"", true);
        validator.validate("\"\" < true", true);
        validator.validate("false <= \"\"", true);
        validator.validate("null <= \"\"", true);

        validator.validate("\"non-empty\" <= \"\"", false);
        validator.validate("\"non-empty\" <= null", false);
        validator.validate("\"non-empty\" > true", false);
        validator.validate("false >= \"non-empty\"", false);
        validator.validate("\"\" >= true", false);
        validator.validate("false > \"\"", false);
        validator.validate("null > \"\"", false);
      });
  }

  @Test
  public void shouldCompareLiteralLong() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("1 <= true", true);
        validator.validate("false < 1", true);
        validator.validate("0 < true", true);
        validator.validate("false <= 0", true);
        validator.validate("null <= 0", true);
        validator.validate("null <= false", true);
        validator.validate("true > null", true);

        validator.validate("1 > true", false);
        validator.validate("false >= 1", false);
        validator.validate("0 >= true", false);
        validator.validate("false > 0", false);
        validator.validate("null > 0", false);
        validator.validate("null > false", false);
        validator.validate("true <= null", false);
      });
  }
}
