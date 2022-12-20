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

package me.blvckbytes.gpeee.error;

import me.blvckbytes.gpeee.parser.expression.FunctionInvocationExpression;
import me.blvckbytes.gpeee.tokenizer.Token;
import org.jetbrains.annotations.Nullable;

public class InvalidFunctionInvocationError extends AEvaluatorError {

  public InvalidFunctionInvocationError(
    FunctionInvocationExpression function,
    @Nullable Integer argumentIndex,
    String message
  ) {
    super(
        getMarkerTarget(function, argumentIndex).getRow(), getMarkerTarget(function, argumentIndex).getCol(),
        function.getFullContainingExpression(),
        message
    );
  }

  private static Token getMarkerTarget(FunctionInvocationExpression expression, @Nullable Integer argumentIndex) {
    if (argumentIndex == null)
      return expression.getHead();

    if (expression.getArguments().size() <= argumentIndex)
      return expression.getHead();

    return expression.getArguments().get(argumentIndex).getA().getHead();
  }
}
