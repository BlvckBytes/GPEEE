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

import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.functions.IStandardFunctionRegistry;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Mapping collections - map
 *
 * Maps a collection of items by running each through a callback expression
 * which will map it to another value and then collects them in the resulting collection.
 */
public class MapFunction extends AStandardFunction {

  @Override
  public Object apply(IEvaluationEnvironment env, List<@Nullable Object> args) {
    // Retrieve arguments
    Iterable<?> items = nonNull(args, 0);
    AExpressionFunction mapper = nonNull(args, 1);
    Object fallback = nullable(args, 2);

    List<Object> result = new ArrayList<>();

    // Loop all items with their indices
    int c = 0;
    for (Object item : items)
      result.add(mapper.apply(env, Arrays.asList(item, c++)));

    if (result.size() == 0 && fallback != null)
      result.add(fallback);

    return result;
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    // map(items, (it, ind) => (..), "empty input")
    return Arrays.asList(
      new ExpressionFunctionArgument("items",     "Items to iterate",             true,  Iterable.class),
      new ExpressionFunctionArgument("mapper",    "Iteration item mapper function",    true,  AExpressionFunction.class),
      new ExpressionFunctionArgument("fallback",  "Fallback when collection is empty", false, String.class)
    );
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("map", this);
  }

  @Override
  public boolean returnsPrimaryResult() {
    return true;
  }
}
