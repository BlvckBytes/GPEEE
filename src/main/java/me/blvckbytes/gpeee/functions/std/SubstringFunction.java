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

import java.util.List;

/**
 * Substring - substring
 *
 * Returns a substring of the input, based on the start- and end indices
 */
public class SubstringFunction extends AStandardFunction {

  @Override
  public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
    String input = nonNull(args, 0);
    Long start = nonNull(args, 1);
    Long end = nullable(args, 2);

    int inputLength = input.length();

    if (start < 0 || start >= inputLength)
      return new FunctionInvocationError(1, "Start index out of bounds");

    if (end != null) {
      if (end < 0 || end >= inputLength)
        return new FunctionInvocationError(1, "End index out of bounds");

      if (end < start)
        return new FunctionInvocationError(1, "Start (" + start + ") cannot be larger than end");

      return input.substring(start.intValue(), end.intValue());
    }

    return input.substring(start.intValue());
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    return List.of(
      new ExpressionFunctionArgument("input", "Input to search in", true, String.class),
      new ExpressionFunctionArgument("start", "Start index (inclusive)", true, Long.class),
      new ExpressionFunctionArgument("end", "End index (exclusive)", false, Long.class)
    );
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("substring", this);
  }

  @Override
  public boolean returnsPrimaryResult() {
    return true;
  }
}
