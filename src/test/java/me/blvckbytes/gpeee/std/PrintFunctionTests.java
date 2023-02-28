/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
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
import me.blvckbytes.gpeee.IExpressionResultValidator;
import me.blvckbytes.gpeee.error.InvalidFunctionArgumentTypeError;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrintFunctionTests {

  @Test
  public void shouldPrintToStdOutAndReturnNull() {
    Object testObject = new Object();

    new EnvironmentBuilder()
      .withStaticVariable("object", testObject)
      .launch(validator -> {
        validatePrinting(validator, "print()", "\n");
        validatePrinting(validator, "print(object)", testObject + "\n");
        validatePrinting(validator, "print(\"Hello\")", "Hello\n");
        validatePrinting(validator, "print(\"Hello\", 25)", "Hello, 25\n");
        validatePrinting(validator, "print(\"Hello\", 25, true)", "Hello, 25, true\n");
      });
  }

  private void validatePrinting(IExpressionResultValidator validator, String expression, String expected) throws Exception {
    PrintStream vanillaOut = System.out;

    try (
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PrintStream printStream = new PrintStream(outputStream);
    ) {
      System.setOut(printStream);
      validator.validate(expression, (Object) null);

      String printed = outputStream.toString(StandardCharsets.UTF_8);

      // NOTE: All calls to validate() create two invocations under the hood, to validate the
      // vanilla- as well as the optimized expression, thus we need to cut off the second half

      int printedLength = printed.length();
      if (printedLength % 2 != 0)
        throw new IllegalStateException("2*n has to be always even");

      assertEquals(expected, printed.substring(0, printedLength / 2));
    } finally {
      System.setOut(vanillaOut);
    }
  }
}
