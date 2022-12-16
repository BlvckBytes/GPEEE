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

import me.blvckbytes.gpeee.error.UndefinedVariableError;
import me.blvckbytes.gpeee.error.UnknownTokenError;
import me.blvckbytes.gpeee.error.UnterminatedStringError;
import org.junit.Test;

public class TerminalTests {

  @Test
  public void shouldEvaluateTerminals() {
    new EnvironmentBuilder()
      .withStaticVariable("var_1", "contents of variable one")
      .launch(validator -> {
        // Long
        validator.validate("3", 3);
        validator.validate(".3", .3);
        validator.validate("-3", -3);
        validator.validateThrows("-3a", UnknownTokenError.class);

        // Double
        validator.validate("3.3", 3.3);
        validator.validate("-3.3", -3.3);
        validator.validateThrows("3..3", UnknownTokenError.class);

        // String
        validator.validate("\"hello, world\"", "hello, world");
        validator.validateThrows("\"no termination", UnterminatedStringError.class);
        validator.validate("\"\\\"\"", "\"");
        validator.validate("\"\\s\"", "'");

        // Literal
        validator.validate("true", true);
        validator.validate("false", false);
        validator.validate("null", null);

        // Variable Identifier
        validator.validate("var_1", "{{var_1}}");
        validator.validateThrows("var_2", UndefinedVariableError.class);
      });
  }
}
