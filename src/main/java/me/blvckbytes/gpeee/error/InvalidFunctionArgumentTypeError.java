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

import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.parser.expression.FunctionInvocationExpression;

public class InvalidFunctionArgumentTypeError extends AEvaluatorError {

  public InvalidFunctionArgumentTypeError(
    FunctionInvocationExpression function,
    ExpressionFunctionArgument definition,
    int argumentIndex,
    Object argumentValue
  ) {
    super(
      // Point to the position of the argument causing trouble or the function identifier if there are no arguments
      decideRow(function, argumentIndex), decideCol(function, argumentIndex),
      function.getFullContainingExpression(),
      "Invalid function argument, expected value of type " + definition.stringifyAllowedTypes() +
      " but got " + (argumentValue == null ? "<null>" : argumentValue.getClass().getName()) + "\n" +
      "Argument description: " + definition.getDescription()
    );
  }

  private static int decideRow(FunctionInvocationExpression function, int argumentIndex) {
    int row = function.getHead().getRow();

    if (argumentIndex < function.getArguments().size()) {
      AExpression argumentExpression = function.getArguments().get(argumentIndex).a;
      row = argumentExpression.getHead().getRow();
    }

    return row;
  }

  private static int decideCol(FunctionInvocationExpression function, int argumentIndex) {
    int col = function.getHead().getCol();

    if (argumentIndex < function.getArguments().size()) {
      AExpression argumentExpression = function.getArguments().get(argumentIndex).a;
      col = argumentExpression.getHead().getCol();
    }

    return col;
  }
}
