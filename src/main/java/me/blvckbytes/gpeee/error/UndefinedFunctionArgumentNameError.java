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

import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.parser.expression.IdentifierExpression;

import java.util.List;
import java.util.stream.Collectors;

public class UndefinedFunctionArgumentNameError extends AEvaluatorError {

  public UndefinedFunctionArgumentNameError(
    AExpressionFunction function,
    IdentifierExpression name
  ) {
    super(
      // Point to the position of the name causing trouble
      name.getHead().getRow(), name.getHead().getCol(),
      name.getFullContainingExpression(),
      "Undefined function argument name >" + name.getSymbol() + "<, needs to be any of: " +
      stringifyAvailableArguments(function)
    );
  }

  /**
   * Stringifies all available function argument names in a human readable format
   * @param function Function to list arguments from
   */
  private static String stringifyAvailableArguments(AExpressionFunction function) {
    List<ExpressionFunctionArgument> args = function.getArguments();

    if (args == null)
      return "This function has no arguments";

    return args.stream()
      .map(ExpressionFunctionArgument::getName)
      .collect(Collectors.joining(" | "));
  }
}
