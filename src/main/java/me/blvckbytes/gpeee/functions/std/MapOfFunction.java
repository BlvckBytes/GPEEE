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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Map from individual key value pairs - map_of
 *
 * Returns a map containing all of the argument pairs passed as inputs.
 */
public class MapOfFunction extends AStandardFunction {

  @Override
  public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
    Map<Object, Object> result = new LinkedHashMap<>();

    if (args.size() % 2 != 0)
      return new FunctionInvocationError(args.size() - 1, "Please provide an even number of arguments");

    for (int i = 0; i < args.size(); i += 2)
      result.put(args.get(i), args.get(i + 1));

    return result;
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    return null;
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("map_of", this);
  }

  @Override
  public boolean returnsPrimaryResult() {
    return false;
  }
}
