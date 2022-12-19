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

public class IfThenElseTests {

  @Test
  public void shouldRouteToTrue() {
    new EnvironmentBuilder()
      .withStaticVariable("x", 5)
      .launch(validator -> {
        validator.validate("if x > 4 then \"Hello\" else \"World\"", "Hello");
        validator.validate("if 1 then \"Hello\" else \"World\"", "Hello");
        validator.validate("if \"string\" then \"Hello\" else \"World\"", "Hello");
      });
  }

  @Test
  public void shouldRouteToFalse() {
    new EnvironmentBuilder()
      .withStaticVariable("x", 5)
      .launch(validator -> {
        validator.validate("if x < 4 then \"Hello\" else \"World\"", "World");
        validator.validate("if 0 then \"Hello\" else \"World\"", "World");
        validator.validate("if \"\" then \"Hello\" else \"World\"", "World");
      });
  }

  @Test
  public void shouldThrowIfMalformed() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("if", UnexpectedTokenError.class);
        validator.validateThrows("if 1", UnexpectedTokenError.class);
        validator.validateThrows("if 1 then", UnexpectedTokenError.class);
        validator.validateThrows("if 1 \"string token\"", UnexpectedTokenError.class);
        validator.validateThrows("if 1 then 2", UnexpectedTokenError.class);
        validator.validateThrows("if 1 then 2 else", UnexpectedTokenError.class);
        validator.validateThrows("if 1 then 2 \"string token\"", UnexpectedTokenError.class);
      });
  }
}
