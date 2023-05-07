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

import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Length of - len
 *
 * Returns the length of it's input argument.
 */
public class LenFunction extends AStandardFunction {

  @Override
  public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
    @Nullable Object input = nullable(args, 0);

    if (input == null)
      return 0;

    if (input instanceof String)
      return ((String) input).length();

    if (input instanceof Collection<?>)
      return ((Collection<?>) input).size();

    if (input instanceof Map<?, ?>)
      return ((Map<?, ?>) input).size();

    if (input.getClass().isArray())
      return Array.getLength(input);

    return 0;
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    return Collections.singletonList(
      new ExpressionFunctionArgument("input", "Input to get the length of", false)
    );
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("len", this);
  }

  @Override
  public boolean returnsPrimaryResult() {
    return true;
  }
}
