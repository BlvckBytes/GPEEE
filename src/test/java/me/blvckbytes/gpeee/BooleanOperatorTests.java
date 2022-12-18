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

public class BooleanOperatorTests {

  @Test
  public void shouldInvert() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("not true", false);
        validator.validate("not false", true);
        validator.validate("not (5 + 3)", false);
        validator.validate("not 0", true);
        validator.validate("not 1", false);
        validator.validate("not 2.2", false);
        validator.validate("not (3 > 1)", false);
        validator.validate("not (3 < 1)", true);
        validator.validate("not \"\"", true);
        validator.validate("not \"content\"", false);
      });
  }

  @Test
  public void shouldApplyConjunction() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("true and false", false);
        validator.validate("false and true", false);
        validator.validate("true and true", true);
        validator.validate("1 and true", true);
        validator.validate("1 and 1", true);

        validator.validate("2 < 5 and 10 > 2", true);
        validator.validate("2 < 5 and 10 > 2 and \"content\"", true);
        validator.validate("2 < 5 and 10 > 2 and \"\"", false);
        validator.validate("2 < 5 and 10 > 2 and 2.3", true);
      });
  }

  @Test
  public void shouldApplyDisjunction() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("true or false", true);
        validator.validate("false or true", true);
        validator.validate("true or true", true);
        validator.validate("1 or true", true);
        validator.validate("0 or 1", true);

        validator.validate("2 > 5 or 10 > 2 or \"content\"", true);
        validator.validate("2 > 5 or 10 < 2 or \"\"", false);
      });
  }

  @Test
  public void shouldApplyCombinations() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("(true or false) and \"content\" and not (0 or \"\")", true);
        validator.validate("(true or false) and \"content\" and not (0 or \"x\")", false);
      });
  }
}
