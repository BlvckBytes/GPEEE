package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.parser.expression.AExpression;
import me.blvckbytes.gpeee.parser.expression.BinaryExpression;
import me.blvckbytes.gpeee.parser.expression.UnaryExpression;

import java.util.function.Consumer;

public class Interpreter {

  private final Consumer<String> debugLogger;

  public Interpreter(Consumer<String> debugLogger) {
    this.debugLogger = debugLogger;
  }

  public ExpressionValue evaluateExpression(AExpression expression, IEvaluationEnvironment environment) throws AInterpreterError {
    if (expression == null)
      return null;

    if (expression instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) expression;
      // TODO: Implement
      return null;
    }

    if (expression instanceof UnaryExpression) {
      UnaryExpression unary = (UnaryExpression) expression;
      // TODO: Implement
      return null;
    }

    throw new IllegalStateException("Cannot parse unknown expression type " + expression.getClass());
  }
}
