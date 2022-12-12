package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import org.jetbrains.annotations.Nullable;

public abstract class AExpression {

  public abstract Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError;

  protected Object evaluateExpression(@Nullable Object maybeExpression, IEvaluationContext context, IValueInterpreter valueInterpreter) {
    if (maybeExpression == null)
      return null;

    if (!(maybeExpression instanceof AExpression))
      return maybeExpression;

    return ((AExpression) maybeExpression).evaluate(context, valueInterpreter);
  }
}
