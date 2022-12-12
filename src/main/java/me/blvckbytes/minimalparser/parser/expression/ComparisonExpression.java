package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.NumberCompare;

@AllArgsConstructor
public class ComparisonExpression extends AExpression {

  private final NumberCompare mode;
  private final Object numberA, numberB;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    Object valueA = evaluateExpression(numberA, context, valueInterpreter);
    Object valueB = evaluateExpression(numberB, context, valueInterpreter);
    return mode.apply(valueA, valueB);
  }

  @Override
  public String toString() {
    return "ComparisonExpression (\n" +
      "mode=" + mode + ",\n" +
      "numberA=" + numberA + ",\n" +
      "numberB=" + numberB + "\n" +
    ')';
  }
}
