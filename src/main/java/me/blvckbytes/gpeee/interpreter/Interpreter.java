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

package me.blvckbytes.gpeee.interpreter;

import lombok.AllArgsConstructor;
import me.blvckbytes.gpeee.DebugLogLevel;
import me.blvckbytes.gpeee.IDebugLogger;
import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.error.UndefinedFunctionError;
import me.blvckbytes.gpeee.error.UndefinedVariableError;
import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.parser.ComparisonOperation;
import me.blvckbytes.gpeee.parser.EqualityOperation;
import me.blvckbytes.gpeee.parser.MathOperation;
import me.blvckbytes.gpeee.parser.expression.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@AllArgsConstructor
public class Interpreter {

  private final IDebugLogger debugLogger;

  public Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    if (expression == null)
      return null;

    debugLogger.log(DebugLogLevel.INTERPRETER, "Evaluating " + expression.getClass().getSimpleName() + ": " + expression.expressionify());

    IValueInterpreter valueInterpreter = environment.getValueInterpreter();

    /////////////////////// Static Values ///////////////////////

    if (expression instanceof LongExpression) {
      debugLogger.log(DebugLogLevel.INTERPRETER, "Taking the immediate long value");
      return ((LongExpression) expression).getNumber();
    }

    if (expression instanceof DoubleExpression) {
      debugLogger.log(DebugLogLevel.INTERPRETER, "Taking the immediate double value");
      return ((DoubleExpression) expression).getValue();
    }

    if (expression instanceof LiteralExpression) {
      debugLogger.log(DebugLogLevel.INTERPRETER, "Taking the immediate literal value");
      return ((LiteralExpression) expression).getValue();
    }

    if (expression instanceof StringExpression) {
      debugLogger.log(DebugLogLevel.INTERPRETER, "Taking the immediate string value");
      return ((StringExpression) expression).getValue();
    }

    ////////////////////// Variable Values //////////////////////

    if (expression instanceof IdentifierExpression) {
      String symbol = ((IdentifierExpression) expression).getSymbol();

      debugLogger.log(DebugLogLevel.INTERPRETER, "Looking up variable " + symbol);

      Object value = environment.getStaticVariables().get(symbol);
      if (value != null) {
        debugLogger.log(DebugLogLevel.INTERPRETER, "Resolved static variable value: " + value);
        return value;
      }

      Supplier<Object> valueSupplier = environment.getLiveVariables().get(symbol);
      if (valueSupplier != null) {
        value = valueSupplier.get();

        if (value != null) {
          debugLogger.log(DebugLogLevel.INTERPRETER, "Resolved dynamic variable value: " + value);
          return value;
        }
      }

      throw new UndefinedVariableError(expression);
    }

    ///////////////////////// Functions /////////////////////////

    if (expression instanceof FunctionInvocationExpression) {
      FunctionInvocationExpression functionExpression = (FunctionInvocationExpression) expression;

      // Try to look up the target function in the environment's function table
      FExpressionFunction function = environment.getFunctions().get(functionExpression.getName().getSymbol());
      if (function == null)
        throw new UndefinedFunctionError(expression);

      debugLogger.log(DebugLogLevel.INTERPRETER, "Evaluating arguments of function invocation " + functionExpression.getName().getSymbol());

      // Evaluate and collect all arguments
      int c = 0;
      List<Object> arguments = new ArrayList<>();
      for (AExpression argument : functionExpression.getArguments()) {
        debugLogger.log(DebugLogLevel.INTERPRETER, "Evaluating argument " + (++c));
        // Evaluate and collect all arguments
        arguments.add(evaluateExpression(argument, environment));
      }

      // Invoke and return that function's result
      Object result = function.apply(environment, arguments);
      debugLogger.log(DebugLogLevel.INTERPRETER, "Invoked function, result: " + result);
      return result;
    }

    if (expression instanceof CallbackExpression) {
      CallbackExpression callbackExpression = (CallbackExpression) expression;

      debugLogger.log(DebugLogLevel.INTERPRETER, "Setting up the java endpoint for a callback expression");

      // This lambda function will be called by java every time the callback is invoked
      return (FExpressionFunction) (env, args) -> {

        // Copy the static variable table and extend it below
        Map<String, Object> combinedVariables = new HashMap<>(environment.getStaticVariables());

        // Map all identifiers from the callback's signature to a matching java argument in sequence
        // If there are more arguments in the signature than provided by java, they'll just be set to null
        for (int i = 0; i < callbackExpression.getSignature().size(); i++) {
          String variableIdentifier = callbackExpression.getSignature().get(i).getSymbol();
          Object variableValue = i < args.size() ? args.get(i) : null;

          debugLogger.log(DebugLogLevel.INTERPRETER, "Adding " + variableIdentifier + "=" + variableValue + " to a callback's environment");
          combinedVariables.put(variableIdentifier, variableValue);
        }

        debugLogger.log(DebugLogLevel.INTERPRETER, "Evaluating a callback's body");

        // Callback expressions are evaluated within their own environment, which extends the current environment
        // by the additional variables coming from the arguments passed by the callback caller
        Object result = evaluateExpression(callbackExpression.getBody(), new IEvaluationEnvironment() {

          @Override
          public Map<String, FExpressionFunction> getFunctions() {
            return environment.getFunctions();
          }

          @Override
          public Map<String, Supplier<Object>> getLiveVariables() {
            return environment.getLiveVariables();
          }

          @Override
          public Map<String, Object> getStaticVariables() {
            return combinedVariables;
          }

          @Override
          public IValueInterpreter getValueInterpreter() {
            return environment.getValueInterpreter();
          }
        });

        debugLogger.log(DebugLogLevel.INTERPRETER, "Callback result=" + result);
        return result;
      };
    }

    //////////////////// Binary Expressions /////////////////////

    if (expression instanceof BinaryExpression) {
      debugLogger.log(DebugLogLevel.INTERPRETER, "Evaluating LHS and RHS of a binary expression");

      Object lhs = evaluateExpression(((BinaryExpression) expression).getLhs(), environment);
      Object rhs = evaluateExpression(((BinaryExpression) expression).getRhs(), environment);

      if (expression instanceof MathExpression) {
        MathOperation operation = ((MathExpression) expression).getOperation();
        Object result;

        result = valueInterpreter.performMath(lhs, rhs, operation);
        debugLogger.log(DebugLogLevel.INTERPRETER, "Math Operation operation " + operation + " result: " + result);
        return result;
      }

      if (expression instanceof EqualityExpression) {
        EqualityOperation operation = ((EqualityExpression) expression).getOperation();
        boolean result;

        switch (operation) {
          case EQUAL:
            result = valueInterpreter.areEqual(lhs, rhs, false);
            break;

          case NOT_EQUAL:
            result = !valueInterpreter.areEqual(lhs, rhs, false);
            break;

          case EQUAL_EXACT:
            result = valueInterpreter.areEqual(lhs, rhs, true);
            break;

          case NOT_EQUAL_EXACT:
            result = !valueInterpreter.areEqual(lhs, rhs, true);
            break;

          default:
            result = false;
            break;
        }

        debugLogger.log(DebugLogLevel.INTERPRETER, "Equality Operation operation " + operation + " result: " + result);
        return result;
      }

      if (expression instanceof ComparisonExpression) {
        ComparisonOperation operation = ((ComparisonExpression) expression).getOperation();
        int comparisonResult = valueInterpreter.compare(lhs, rhs);
        boolean result;

        switch (operation) {
          case LESS_THAN:
            result = comparisonResult < 0;
            break;

          case GREATER_THAN:
            result = comparisonResult > 0;
            break;

          case LESS_THAN_OR_EQUAL:
            result = comparisonResult <= 0;
            break;

          case GREATER_THAN_OR_EQUAL:
            result = comparisonResult >= 0;
            break;

          default:
            result = false;
            break;
        }

        debugLogger.log(DebugLogLevel.INTERPRETER, "Comparison Operation operation " + operation + " result: " + result);
        return result;
      }

      if (expression instanceof ConjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) && valueInterpreter.asBoolean(rhs);
        debugLogger.log(DebugLogLevel.INTERPRETER, "Conjunction Operation result: " + result);
        return result;
      }

      if (expression instanceof DisjunctionExpression) {
        boolean result = valueInterpreter.asBoolean(lhs) || valueInterpreter.asBoolean(rhs);
        debugLogger.log(DebugLogLevel.INTERPRETER, "Disjunction Operation result: " + result);
        return result;
      }

      if (expression instanceof ConcatenationExpression) {
        String result = valueInterpreter.asString(lhs) + valueInterpreter.asString(rhs);
        debugLogger.log(DebugLogLevel.INTERPRETER, "Concatenation Operation result: " + result);
        return result;
      }
    }

    ///////////////////// Unary Expressions /////////////////////

    if (expression instanceof UnaryExpression) {
      debugLogger.log(DebugLogLevel.INTERPRETER, "Evaluating input of a unary expression");

      Object input = evaluateExpression(((UnaryExpression) expression).getInput(), environment);

      if (expression instanceof FlipSignExpression) {
        Object result = -1 * (valueInterpreter.hasDecimalPoint(input) ? valueInterpreter.asDouble(input) : valueInterpreter.asLong(input));
        debugLogger.log(DebugLogLevel.INTERPRETER, "Flip Sign Operation result: " + result);
        return result;
      }

      if (expression instanceof InvertExpression) {
        boolean result = !valueInterpreter.asBoolean(input);
        debugLogger.log(DebugLogLevel.INTERPRETER, "Invert Operation result: " + result);
        return result;
      }
    }

    throw new IllegalStateException("Cannot parse unknown expression type " + expression.getClass());
  }
}