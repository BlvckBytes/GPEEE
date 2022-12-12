package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.MathOperation;

@AllArgsConstructor
public class MathOperationExpression extends AExpression {

  private final MathOperation operation;
  private final Object numberA, numberB;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    Object valueA = evaluateExpression(numberA, context, valueInterpreter);
    Object valueB = evaluateExpression(numberB, context, valueInterpreter);
    return operation.apply(valueA, valueB, valueInterpreter);
  }

  @Override
  public String toString() {
    return "MathOperationExpression (\n" +
      "operation=" + operation + ",\n" +
      "numberA=" + numberA + ",\n" +
      "numberB=" + numberB + "\n" +
    ')';
  }
}
