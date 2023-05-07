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

package me.blvckbytes.gpeee.functions.std;

import me.blvckbytes.gpeee.error.FunctionInvocationError;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

/**
 * Date format - date_format
 *
 * Returns the formatted date based on the timestamp input
 */
public class DateFormatFunction extends AStandardFunction {

  private final Map<String, Map<TimeZone, DateFormat>> dateFormatByStringFormatAndZone;

  public DateFormatFunction() {
    this.dateFormatByStringFormatAndZone = new HashMap<>();
  }

  @Override
  public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
    Object date = nonNull(args, 0);
    String type = nonNull(args, 1);
    String formatString = nonNull(args, 2);
    String timeZone = nullableWithFallback(args, 3, "UTC");

    TimeZone formatTimeZone;
    try {
      ZoneId timeZoneId = ZoneId.of(timeZone);
      formatTimeZone = TimeZone.getTimeZone(timeZoneId);
    } catch (Exception e) {
      return new FunctionInvocationError(3, "Invalid timezone provided");
    }

    DateFormat format;
    try {
      format = getDateFormat(formatString, formatTimeZone);
    } catch (Exception e) {
      return new FunctionInvocationError(2, "Malformed date format");
    }

    type = type.toLowerCase(Locale.ROOT);

    if (type.equals("date")) {
      if (!(date instanceof Date))
        return new FunctionInvocationError(0, "Not an instance of Date");

      return format.format((Date) date);
    }

    boolean isMillis = type.equals("millis");
    if (type.equals("seconds") || isMillis) {
      if (!(date instanceof Number))
        return new FunctionInvocationError(0, "Not an instance of Number");

      long stamp = ((Number) date).longValue();

      if (!isMillis)
        stamp *= 1000;

      return format.format(new Date(stamp));
    }

    return new FunctionInvocationError(1, "Invalid date type provided");
  }

  private DateFormat getDateFormat(String format, TimeZone zone) {
    Map<TimeZone, DateFormat> dateFormatByZone = this.dateFormatByStringFormatAndZone.computeIfAbsent(format, key -> new HashMap<>());

    DateFormat dateFormat = dateFormatByZone.get(zone);

    if (dateFormat != null)
      return dateFormat;

    dateFormat = new SimpleDateFormat(format);
    dateFormat.setTimeZone(zone);
    dateFormatByZone.put(zone, dateFormat);

    return dateFormat;
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    return Arrays.asList(
      new ExpressionFunctionArgument("date",     "Input date to format",                                     true),
      new ExpressionFunctionArgument("type",     "Type of date provided (seconds, millis, date)",            true,  String.class),
      new ExpressionFunctionArgument("format",   "Date format to apply",                                     true,  String.class),
      new ExpressionFunctionArgument("timezone", "Timezone to use, defaults to UTC",                         false, String.class)
    );
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("date_format", this);
  }

  @Override
  public boolean returnsPrimaryResult() {
    return true;
  }
}
