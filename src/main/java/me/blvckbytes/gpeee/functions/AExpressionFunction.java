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

package me.blvckbytes.gpeee.functions;

import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.error.InvalidFunctionArgumentError;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import me.blvckbytes.gpeee.parser.expression.FunctionInvocationExpression;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public abstract class AExpressionFunction {

  /**
   * Called whenever a function call to the registered corresponding
   * identifier is performed within an expression
   * @param environment A reference to the current environment
   * @param args Arguments supplied by the invocation
   * @return Return value of this function
   */
  public abstract Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args);

  /**
   * Specifies the function arguments in order and how they have to be used in
   * order to make up a valid function call. If this method returns null,
   * the function call will not be checked.
   */
  public abstract @Nullable List<ExpressionFunctionArgument> getArguments();

  /**
   * Validates the provided list of arguments against the locally kept argument definitions
   * and throws a detailed {@link InvalidFunctionArgumentError} when an argument mismatches.
   * @param expression Expression for error context
   * @param valueInterpreter Reference to the currently in-use value interpreter for possible auto-conversions
   * @param args Arguments to validate
   * @throws InvalidFunctionArgumentError Thrown when an argument mismatches it's corresponding definition
   */
  public void validateArguments(FunctionInvocationExpression expression, IValueInterpreter valueInterpreter, List<@Nullable Object> args) throws InvalidFunctionArgumentError {
    List<ExpressionFunctionArgument> argumentDefinitions = getArguments();

    // No definitions available, cannot validate, call passes
    if (argumentDefinitions == null)
      return;

    // Check all arguments one by one in order
    for (int i = 0; i < argumentDefinitions.size(); i++) {
      ExpressionFunctionArgument definition = argumentDefinitions.get(i);
      Object argument = i >= args.size() ? null : args.get(i);
      Tuple<Boolean, @Nullable Object> result = definition.checkDescriptionAndPossiblyConvert(argument, valueInterpreter);

      // Value did not pass all checks and could not be auto-converted either
      if (!result.getA())
        throw new InvalidFunctionArgumentError(expression, definition, i, argument);

      // Update the value within the list to the possibly converted value
      if (i < args.size())
        args.set(i, result.getB());
    }
  }

  /**
   * Create a new unchecked expression function handler
   * @param handler Handler of function calls
   */
  public static AExpressionFunction makeUnchecked(BiFunction<IEvaluationEnvironment, List<@Nullable Object>, Object> handler) {
    return new AExpressionFunction() {

      @Override
      public Object apply(IEvaluationEnvironment environment, List<@Nullable Object> args) {
        // Relay to the lambda handler
        return handler.apply(environment, args);
      }

      @Override
      public @Nullable List<ExpressionFunctionArgument> getArguments() {
        // Invocation arguments remain unchecked
        return null;
      }
    };
  }
}
