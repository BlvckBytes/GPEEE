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
import me.blvckbytes.gpeee.error.InvalidFunctionArgumentTypeError;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.interpreter.IValueInterpreter;
import me.blvckbytes.gpeee.parser.expression.FunctionInvocationExpression;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class AExpressionFunction {

  //=========================================================================//
  //                             Abstract Methods                            //
  //=========================================================================//

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

  //=========================================================================//
  //                                  Utilities                              //
  //=========================================================================//

  /**
   * Get a known non-null argument from the argument list
   * @param args Argument list to read from
   * @param index Index of the known argument
   * @return Argument, required to be non-null
   */
  @SuppressWarnings("unchecked")
  protected<T> T nonNull(List<@Nullable Object> args, int index) {
    return (T) Objects.requireNonNull(args.get(index));
  }

  /**
   * Get a maybe null argument from the argument list
   * @param args Argument list to read from
   * @param index Index of the argument
   * @return Argument from the list or null if the index has been out-of-range
   */
  @SuppressWarnings("unchecked")
  protected<T> @Nullable T nullable(List<@Nullable Object> args, int index) {
    return (T) (index >= args.size() ? null : args.get(index));
  }

  /**
   * Get a maybe null argument from the argument list or use the provided fallback
   * @param args Argument list to read from
   * @param index Index of the argument
   * @param fallback Fallback to use in the situation of an absent argument
   * @return Argument from the list or null if the index has been out-of-range
   */
  @SuppressWarnings("unchecked")
  protected<T> T nullableWithFallback(List<@Nullable Object> args, int index, T fallback) {
    T result = (T) (index >= args.size() ? fallback : args.get(index));

    if (result == null)
      return fallback;

    return result;
  }

  //=========================================================================//
  //                               Internal API                              //
  //=========================================================================//

  /**
   * Validates the provided list of arguments against the locally kept argument definitions
   * and throws a detailed {@link InvalidFunctionArgumentTypeError} when an argument mismatches.
   * @param expression Expression for error context
   * @param valueInterpreter Reference to the currently in-use value interpreter for possible auto-conversions
   * @param args Arguments to validate
   * @throws InvalidFunctionArgumentTypeError Thrown when an argument mismatches it's corresponding definition
   */
  public void validateArguments(FunctionInvocationExpression expression, IValueInterpreter valueInterpreter, List<@Nullable Object> args) throws InvalidFunctionArgumentTypeError {
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
      if (!result.a)
        throw new InvalidFunctionArgumentTypeError(expression, definition, i, argument);

      // Update the value within the list to the possibly converted value
      if (i < args.size())
        args.set(i, result.b);
    }
  }
}
