package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

@Getter
@AllArgsConstructor
public class BooleanOrExpression extends AExpression {

  private final Object booleanA, booleanB;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    Object valueA = evaluateExpression(booleanA, context, valueInterpreter);
    Object valueB = evaluateExpression(booleanB, context, valueInterpreter);
    return valueInterpreter.isTruthy(valueA) || valueInterpreter.isTruthy(valueB);
  }

  @Override
  public String toString() {
    return "BooleanOrExpression (\n" +
      "booleanA=" + booleanA + ",\n" +
      "booleanB=" + booleanB + "\n" +
    ')';
  }
}
