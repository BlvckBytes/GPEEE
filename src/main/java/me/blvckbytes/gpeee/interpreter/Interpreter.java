package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.error.UndefinedFunctionError;
import me.blvckbytes.gpeee.error.UndefinedVariableError;
import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.parser.expression.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Interpreter {

  private static final IValueInterpreter STD_VALUE_INTERPRETER = new StandardValueInterpreter();

  public Object evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    if (expression == null)
      return null;

    IValueInterpreter valueInterpreter = getValueInterpreter(environment);

    /////////////////////// Static Values ///////////////////////

    if (expression instanceof LongExpression)
      return ((LongExpression) expression).getNumber();

    if (expression instanceof DoubleExpression)
      return ((DoubleExpression) expression).getValue();

    if (expression instanceof LiteralExpression)
      return ((LiteralExpression) expression).getValue();

    if (expression instanceof StringExpression)
      return ((StringExpression) expression).getValue();

    ////////////////////// Variable Values //////////////////////

    if (expression instanceof IdentifierExpression) {
      String symbol = ((IdentifierExpression) expression).getSymbol();

      Object value = environment.getStaticVariables().get(symbol);
      if (value != null)
        return value;

      Supplier<Object> valueSupplier = environment.getLiveVariables().get(symbol);
      if (valueSupplier != null) {
        value = valueSupplier.get();

        if (value == null)
          throw new UndefinedVariableError(expression);

        return value;
      }
    }

    ///////////////////////// Functions /////////////////////////

    if (expression instanceof FunctionInvocationExpression) {
      FunctionInvocationExpression functionExpression = (FunctionInvocationExpression) expression;

      // Try to look up the target function in the environment's function table
      FExpressionFunction function = environment.getFunctions().get(functionExpression.getName().getSymbol());
      if (function == null)
        throw new UndefinedFunctionError(expression);

      // Evaluate and collect all arguments
      List<Object> arguments = new ArrayList<>();
      for (AExpression argument : functionExpression.getArguments())
        arguments.add(evaluateExpression(argument, environment));

      // Invoke and return that function's result
      return function.apply(arguments);
    }

    if (expression instanceof CallbackExpression) {
      CallbackExpression callbackExpression = (CallbackExpression) expression;

      // This function (wrapped as a value) will be called by java every time the caller invokes it
      return (FExpressionFunction) args -> {

        // Copy the static variable table and extend it below
        Map<String, Object> combinedVariables = new HashMap<>(environment.getStaticVariables());

        // Map all identifiers from the callback's signature to a matching java argument in sequence
        // If there are more arguments in the signature than provided by java, they'll just be set to null
        for (int i = 0; i < callbackExpression.getSignature().size(); i++) {
          combinedVariables.put(
            callbackExpression.getSignature().get(i).getSymbol(),
            (i < args.size() ? args.get(i) : null)
          );
        }

        // Callback expressions are evaluated within their own environment, which extends the current environment
        // by the additional variables coming from the arguments passed by the callback caller
        return evaluateExpression(callbackExpression.getBody(), new IEvaluationEnvironment() {

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
          public @Nullable IValueInterpreter getValueInterpreter() {
            return environment.getValueInterpreter();
          }
        });
      };
    }

    //////////////////// Binary Expressions /////////////////////

    if (expression instanceof BinaryExpression) {
      Object lhs = evaluateExpression(((BinaryExpression) expression).getLhs(), environment);
      Object rhs = evaluateExpression(((BinaryExpression) expression).getRhs(), environment);

      if (expression instanceof MathExpression)
        return valueInterpreter.performMath(lhs, rhs, ((MathExpression) expression).getOperation());

      if (expression instanceof EqualityExpression) {
        switch (((EqualityExpression) expression).getOperation()) {
          case EQUAL:
            return valueInterpreter.areEqual(lhs, rhs, false);
          case NOT_EQUAL:
            return !valueInterpreter.areEqual(lhs, rhs, false);
          case EQUAL_EXACT:
            return valueInterpreter.areEqual(lhs, rhs, true);
          case NOT_EQUAL_EXACT:
            return !valueInterpreter.areEqual(lhs, rhs, true);
          default:
            return false;
        }
      }

      if (expression instanceof ComparisonExpression) {
        int comparisonResult = valueInterpreter.compare(lhs, rhs);

        switch (((ComparisonExpression) expression).getOperation()) {
          case LESS_THAN:
            return comparisonResult < 0;
          case GREATER_THAN:
            return comparisonResult > 0;
          case LESS_THAN_OR_EQUAL:
            return comparisonResult <= 0;
          case GREATER_THAN_OR_EQUAL:
            return comparisonResult >= 0;
          default:
            return false;
        }
      }

      if (expression instanceof ConjunctionExpression)
        return valueInterpreter.asBoolean(lhs) && valueInterpreter.asBoolean(rhs);

      if (expression instanceof DisjunctionExpression)
        return valueInterpreter.asBoolean(lhs) || valueInterpreter.asBoolean(rhs);

      if (expression instanceof ConcatenationExpression)
        return valueInterpreter.asString(lhs) + valueInterpreter.asString(rhs);
    }

    ///////////////////// Unary Expressions /////////////////////

    if (expression instanceof UnaryExpression) {
      Object input = evaluateExpression(((UnaryExpression) expression).getInput(), environment);

      if (expression instanceof FlipSignExpression)
        return -1 * (valueInterpreter.hasDecimalPoint(input) ? valueInterpreter.asDouble(input) : valueInterpreter.asLong(input));

      if (expression instanceof InvertExpression)
        return !valueInterpreter.asBoolean(input);
    }

    throw new IllegalStateException("Cannot parse unknown expression type " + expression.getClass());
  }

  private @NotNull IValueInterpreter getValueInterpreter(IEvaluationEnvironment environment) {
    IValueInterpreter environmentValueInterpreter = environment.getValueInterpreter();
    return environmentValueInterpreter == null ? STD_VALUE_INTERPRETER : environmentValueInterpreter;
  }
}