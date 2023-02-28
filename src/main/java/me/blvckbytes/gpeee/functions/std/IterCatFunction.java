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

import java.util.Collection;
import java.util.List;

/**
 * Iteration concatenation - iter_cat
 *
 * Concatenates a collection of items by running each through a callback expression
 * which will format it to a string value and then joins them all with the provided
 * separator. If there are no items, the fallback will be returned only.
 */
public class IterCatFunction extends AStandardFunction {

  @Override
  public Object apply(IEvaluationEnvironment env, List<@Nullable Object> args) {
    // Retrieve arguments
    Collection<?> items = nonNull(args, 0);
    AExpressionFunction mapper = nonNull(args, 1);
    @Nullable String separator = nullable(args, 2);
    @Nullable String fallback = nullable(args, 3);

    // Fall back on a sensible default
    if (separator == null)
      separator = ", ";

    StringBuilder result = new StringBuilder();

    // Loop all items with their indices
    int c = 0;
    for (Object item : items) {
      result.append(result.length() == 0 ? "" : separator).append(
        mapper.apply(env, List.of(item, c++))
      );
    }

    // No items available but a fallback string has been supplied
    if (items.size() == 0 && fallback != null)
      return fallback;

    // Respond with the built-up result
    return result.toString();
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    // iter_cat(items, (it, ind) => (..), "separator", "no items fallback")
    return List.of(
      new ExpressionFunctionArgument("items",     "Collection to iterate",             true,  Collection.class),
      new ExpressionFunctionArgument("mapper",    "Iteration item mapper function",    true,  AExpressionFunction.class),
      new ExpressionFunctionArgument("separator", "Item separator",                    false, String.class),
      new ExpressionFunctionArgument("fallback",  "Fallback when collection is empty", false, String.class)
    );
  }

  @Override
  public void registerSelf(IStandardFunctionRegistry registry) {
    registry.register("iter_cat", this);
  }

  @Override
  public boolean returnsPrimaryResult() {
    return true;
  }
}
