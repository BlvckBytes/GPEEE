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

import me.blvckbytes.gpeee.error.*;
import org.junit.jupiter.api.Test;

public class TerminalTests {

  @Test
  public void shouldEvaluateTerminals() {
    EnvironmentBuilder env = new EnvironmentBuilder()
      .withStaticVariable("var_1", "contents of variable one");

    env.launch(validator -> {
      // Long
      validator.validate("3", 3);
      validator.validate(".3", .3);
      validator.validate("-3", -3);
      validator.validateThrows("-3a", UndefinedVariableError.class);
      validator.validateThrows("3e", UndefinedVariableError.class);
      validator.validate("3e3", 3 * (int) Math.pow(10, 3));
      validator.validate("-3e3", -3 * (int) Math.pow(10, 3));
      validator.validateThrows("3e-3", NegativeExponentOnLongError.class);

      // Double
      validator.validate("3.3", 3.3);
      validator.validate("-3.3", -3.3);
      validator.validateThrows("3.3e", UndefinedVariableError.class);
      validator.validate("3.3e3", 3.3 * Math.pow(10, 3));
      validator.validate("-3.3e3", -3.3 * Math.pow(10, 3));
      validator.validate("-3.3e-3", -3.3 * Math.pow(10, -3));

      // Would be parsed as a member access of the member .3 on 3
      validator.validateThrows("3..3", UnknownMemberError.class);

      // String
      validator.validate("\"hello, world\"", "hello, world");
      validator.validateThrows("\"no termination", UnterminatedStringError.class);

      // Escaped double quote
      validator.validate("\"\\\"\"", "\"");

      // Escape sequence for a single quote
      validator.validate("\"\\s\"", "'");

      // Escaped escape sequence for a single quote
      validator.validate("\"\\\\s\"", "\\s");

      // Just a backslash, should remain untouched
      validator.validate("\"\\ \"", "\\ ");

      // Two backslashes should cancel out and not escape the string end quote
      validator.validate("\"\\\\\"", "\\");

      // Just an s, should remain untouched
      validator.validate("\"s\"", "s");

      // Literal
      validator.validate("true", true);
      validator.validate("false", false);
      validator.validate("null", (Object) null);

      // Should throw on empty expressions
      validator.validateThrows("", UnexpectedTokenError.class);

      // Should ignore comments
      validator.validate("# This is a comment\nfalse # trailing comment", false);

      // Should eat tabs too
      validator.validate("\t\tfalse\t", false);

      // Variable Identifier
      validator.validate("var_1", env.getVariable("var_1"));

      // Ignore casing on identifiers
      validator.validate("VaR_1", env.getVariable("var_1"));

      // Throw on invalid identifier
      validator.validateThrows("var_2", UndefinedVariableError.class);
    });
  }
}
