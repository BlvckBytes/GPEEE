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
import me.blvckbytes.gpeee.error.InvalidFunctionArgumentTypeError;
import me.blvckbytes.gpeee.error.InvalidFunctionInvocationError;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class DateFormatFunctionTests {

  @Test
  public void shouldRequireArguments() {
    new EnvironmentBuilder()
      .launch(validator -> {
        validator.validateThrows("date_format()", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("date_format(0)", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("date_format(0, \"\")", InvalidFunctionArgumentTypeError.class);
      });
  }

  @Test
  public void shouldThrowOnNonNumericValues() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .launch(validator -> {
        // Required, thus non-nullable
        validator.validateThrows("date_format(null, \"seconds\", format_a)", InvalidFunctionArgumentTypeError.class);
        validator.validateThrows("date_format(null, \"millis\", format_a)", InvalidFunctionArgumentTypeError.class);

        validator.validateThrows("date_format(\"\", \"seconds\", format_a)", InvalidFunctionInvocationError.class);
        validator.validateThrows("date_format(true, \"seconds\", format_a)", InvalidFunctionInvocationError.class);
        validator.validateThrows("date_format(\"\", \"millis\", format_a)", InvalidFunctionInvocationError.class);
        validator.validateThrows("date_format(true, \"millis\", format_a)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnNonMalformedFormat() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "hello, world")
      .launch(validator -> {
        validator.validateThrows("date_format(0, \"millis\", format)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnMalformedTimeZone() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "HH:mm")
      .withStaticVariable("zone", "hello")
      .launch(validator -> {
        validator.validateThrows("date_format(0, \"millis\", format, zone)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnMalformedType() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "HH:mm")
      .launch(validator -> {
        validator.validateThrows("date_format(0, \"hello\", format)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldThrowOnNonDateObject() {
    new EnvironmentBuilder()
      .withStaticVariable("format", "HH:mm")
      .withStaticVariable("non_date", new Object())
      .launch(validator -> {
        validator.validateThrows("date_format(non_date, \"date\", format)", InvalidFunctionInvocationError.class);
      });
  }

  @Test
  public void shouldFormatSeconds() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .withStaticVariable("format_b", "yyyy/MM/dd")
      .withStaticVariable("format_c", "dd.MM.yyyy HH:mm:ss")
      .withStaticVariable("stamp", 1677579422) // Tue Feb 28 2023 10:17:02 UTC
      .launch(validator -> {
        validator.validate("date_format(stamp, \"seconds\", format_a)", "2023-02-28");
        validator.validate("date_format(stamp, \"seconds\", format_b)", "2023/02/28");
        validator.validate("date_format(stamp, \"seconds\", format_c)", "28.02.2023 10:17:02");

        validator.validate("date_format(stamp, \"seconds\", format_c, \"CET\")", "28.02.2023 11:17:02");
      });
  }

  @Test
  public void shouldFormatMilliSeconds() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .withStaticVariable("format_b", "yyyy/MM/dd")
      .withStaticVariable("format_c", "dd.MM.yyyy HH:mm:ss")
      .withStaticVariable("stamp", 1677579422L * 1000) // Tue Feb 28 2023 10:17:02 UTC
      .launch(validator -> {
        validator.validate("date_format(stamp, \"millis\", format_a)", "2023-02-28");
        validator.validate("date_format(stamp, \"millis\", format_b)", "2023/02/28");
        validator.validate("date_format(stamp, \"millis\", format_c)", "28.02.2023 10:17:02");

        validator.validate("date_format(stamp, \"millis\", format_c, \"CET\")", "28.02.2023 11:17:02");
      });
  }

  @Test
  public void shouldFormatDates() {
    new EnvironmentBuilder()
      .withStaticVariable("format_a", "yyyy-MM-dd")
      .withStaticVariable("format_b", "yyyy/MM/dd")
      .withStaticVariable("format_c", "dd.MM.yyyy HH:mm:ss")
      .withStaticVariable("date", new Date(1677579422L * 1000)) // Tue Feb 28 2023 10:17:02 UTC
      .launch(validator -> {
        validator.validate("date_format(date, \"date\", format_a)", "2023-02-28");
        validator.validate("date_format(date, \"date\", format_b)", "2023/02/28");
        validator.validate("date_format(date, \"date\", format_c)", "28.02.2023 10:17:02");

        validator.validate("date_format(date, \"date\", format_c, \"CET\")", "28.02.2023 11:17:02");
      });
  }
}
