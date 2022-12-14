package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.error.UndefinedFunctionError;
import me.blvckbytes.gpeee.error.UndefinedVariableError;
import me.blvckbytes.gpeee.functions.FExpressionFunction;
import me.blvckbytes.gpeee.parser.expression.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Interpreter {

  private static final ExpressionValue NEGATIVE_ONE = ExpressionValue.fromInteger(-1);
  private static final ExpressionValue BOOLEAN_FALSE = ExpressionValue.fromBoolean(false);

  public ExpressionValue evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AEvaluatorError {
    if (expression == null)
      return null;

    /////////////////////// Static Values ///////////////////////

    if (expression instanceof IntExpression)
      return ((IntExpression) expression).getNumber();

    if (expression instanceof DoubleExpression)
      return ((DoubleExpression) expression).getValue();

    if (expression instanceof LiteralExpression)
      return ((LiteralExpression) expression).getValue();

    if (expression instanceof StringExpression)
      return ((StringExpression) expression).getValue();

    ////////////////////// Variable Values //////////////////////

    if (expression instanceof IdentifierExpression) {
      String symbol = ((IdentifierExpression) expression).getSymbol();

      ExpressionValue value = environment.getStaticVariables().get(symbol);
      if (value != null)
        return value;

      Supplier<ExpressionValue> valueSupplier = environment.getLiveVariables().get(symbol);
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
      List<ExpressionValue> arguments = new ArrayList<>();
      for (AExpression argument : functionExpression.getArguments())
        arguments.add(evaluateExpression(argument, environment));

      // Invoke and return that function's result
      return function.apply(arguments);
    }

    if (expression instanceof CallbackExpression) {
      CallbackExpression callbackExpression = (CallbackExpression) expression;

      // This function (wrapped as a value) will be called by java every time the caller invokes it
      return ExpressionValue.fromFunction(args -> {

        // Copy the static variable table and extend it below
        Map<String, ExpressionValue> combinedVariables = new HashMap<>(environment.getStaticVariables());

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
          public Map<String, Supplier<ExpressionValue>> getLiveVariables() {
            return environment.getLiveVariables();
          }

          @Override
          public Map<String, ExpressionValue> getStaticVariables() {
            return combinedVariables;
          }
        });
      });
    }

    //////////////////// Binary Expressions /////////////////////

    if (expression instanceof BinaryExpression) {
      ExpressionValue lhs = evaluateExpression(((BinaryExpression) expression).getLhs(), environment);
      ExpressionValue rhs = evaluateExpression(((BinaryExpression) expression).getRhs(), environment);

      if (expression instanceof MathExpression) {
        switch (((MathExpression) expression).getOperation()) {
          case ADDITION:
            return lhs.add(rhs);
          case SUBTRACTION:
            return lhs.subtract(rhs);
          case MULTIPLICATION:
            return lhs.multiply(rhs);
          case DIVISION:
            return lhs.divide(rhs);
          case MODULO:
            return lhs.modulo(rhs);
          case POWER:
            return lhs.power(rhs);
          default:
            return null;
        }
      }

      if (expression instanceof EqualityExpression) {
        switch (((EqualityExpression) expression).getOperation()) {
          case EQUAL:
            return ExpressionValue.fromBoolean(lhs.equalsTo(rhs, false));
          case NOT_EQUAL:
            return ExpressionValue.fromBoolean(!lhs.equalsTo(rhs, false));
          case EQUAL_EXACT:
            return ExpressionValue.fromBoolean(lhs.equalsTo(rhs, true));
          case NOT_EQUAL_EXACT:
            return ExpressionValue.fromBoolean(!lhs.equalsTo(rhs, true));
          default:
            return BOOLEAN_FALSE;
        }
      }

      if (expression instanceof ComparisonExpression) {
        int comparisonResult = lhs.compareTo(rhs);

        switch (((ComparisonExpression) expression).getOperation()) {
          case LESS_THAN:
            return ExpressionValue.fromBoolean(comparisonResult < 0);
          case GREATER_THAN:
            return ExpressionValue.fromBoolean(comparisonResult > 0);
          case LESS_THAN_OR_EQUAL:
            return ExpressionValue.fromBoolean(comparisonResult <= 0);
          case GREATER_THAN_OR_EQUAL:
            return ExpressionValue.fromBoolean(comparisonResult >= 0);
          default:
            return BOOLEAN_FALSE;
        }
      }

      if (expression instanceof ConjunctionExpression)
        return lhs.and(rhs);

      if (expression instanceof DisjunctionExpression)
        return lhs.or(rhs);

      if (expression instanceof ConcatenationExpression)
        return lhs.concatenate(rhs);
    }

    ///////////////////// Unary Expressions /////////////////////

    if (expression instanceof FlipSignExpression)
      return evaluateExpression(((FlipSignExpression) expression).getInput(), environment).multiply(NEGATIVE_ONE);

    if (expression instanceof InvertExpression)
      return evaluateExpression(((InvertExpression) expression).getInput(), environment).invert();

    throw new IllegalStateException("Cannot parse unknown expression type " + expression.getClass());
  }
}
