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

import me.blvckbytes.gpeee.error.UnexpectedTokenError;
import org.junit.jupiter.api.Test;

public class MathOperatorTests {

  @Test
  public void shouldEvaluateSignsProperly() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5 * -3", 5 * -3);
        validator.validate("-5 * -3", -5 * -3);
        validator.validate("-5 * 3", -5 * 3);

        validator.validate("5.2 * -3.1", 5.2 * -3.1);
        validator.validate("-5.2 * -3.1", -5.2 * -3.1);
        validator.validate("-5.2 * 3.1", -5.2 * 3.1);

        validator.validate("2 - -2", 4);
        validator.validate("-2 - -2", 0);
        validator.validate("-(2 - -2)", -4);
        validator.validate("-2 + 2", 0);
        validator.validate("2 + 2", 4);
      });
  }

  @Test
  public void shouldEvaluateMultiplicationBeforeAddition() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("5 + 2 * 3", 5 + (2 * 3));
        validator.validate("5 * 2 + 3", (5 * 2) + 3);
        validator.validate("5 + 2 / 3 - 2", 5 + (2.0 / 3) - 2);
      });
  }

  @Test
  public void shouldEvaluateExponentiationBeforeAddition() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("2^3 + 1", Math.pow(2, 3) + 1);
        validator.validate("1 - 2^3 * 3 * 4 * 5", 1 - Math.pow(2, 3) * 3 * 4 * 5);
      });
  }

  @Test
  public void shouldEvaluateExponentiationBeforeMultiplication() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("2^3 * 1", Math.pow(2, 3) * 1);
        validator.validate("1 / 2^3 * 3", 1 / Math.pow(2, 3) * 3);
      });
  }

  @Test
  public void shouldEvaluateParenthesesBeforeEverythingElse() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("(2 + 3) % 4", (2 + 3) % 4);
        validator.validate("4 * (2 + 3) * 2", 4 * (2 + 3) * 2);
        validator.validate("5 / (2 * 3)", 5.0 / (2 * 3));
        validator.validate("4^(3 + 2)", Math.pow(4, 3 + 2));
        validator.validate("4^(3 * (3 - 1))", Math.pow(4, 3 * (3 - 1)));
      });
  }

  @Test
  public void shouldThrowOnMalformedParentheses() {
    new EnvironmentBuilder()
      .launch(validator -> {
        // Parenthesis not closed
        validator.validateThrows("3 / (2 + 3", UnexpectedTokenError.class);

        // Extra token after a "completed" expression, should return the value of that expression
        validator.validate("3 / (2 + 3) 5", 5);
      });
  }

  @Test
  public void shouldEvaluateRoots() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validate("3^-(1/2)", 1.0 / Math.sqrt(3));
        validator.validate("2^(1/2)", Math.sqrt(2));
        validator.validate("22^-(1/3)", 1.0 / Math.pow(22.0, 1.0 / 3.0));
        validator.validate("127^(1/4)", Math.pow(127.0, 1.0 / 4.0));
      });
  }

  @Test
  public void shouldTruncateDoublesIfAppropriate() {
    new EnvironmentBuilder()
      .launch(validator -> {
        // Cannot truncate due to decimal point of importance
        validator.validate("2.0 + 2.1", 2.0 + 2.1);
        validator.validate("2.0 - 2.1", 2.0 - 2.1);
        validator.validate("2.0 * 2.1", 2.0 * 2.1);
        validator.validate("2.0 / 2.1", 2.0 / 2.1);
        validator.validate("2.0 % 2.1", 2.0 % 2.1);
        validator.validate("2.0 ^ 2.1", Math.pow(2.0, 2.1));

        // Can truncate since the decimal point does nothing
        validator.validateExact("2.0 + 2.0", 4L);
        validator.validateExact("2.0 - 2.0", 0L);
        validator.validateExact("2.0 * 2.0", 4L);
        validator.validateExact("2.0 / 2.0", 1L);
        validator.validateExact("4.0 % 3.0", 1L);
        validator.validateExact("2.0 ^ 2.0", 4L);

        // Cannot truncate as the division result is not whole
        validator.validateExact("2.0 / 3.0", 2.0 / 3.0);
      });
  }
}
