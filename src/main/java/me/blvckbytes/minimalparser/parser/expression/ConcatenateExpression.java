package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

@Getter
@AllArgsConstructor
public class ConcatenateExpression extends AExpression {

  private final Object valueA, valueB;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    Object stringA = evaluateExpression(valueA, context, valueInterpreter);
    Object stringB = evaluateExpression(valueB, context, valueInterpreter);
    return stringA + String.valueOf(stringB);
  }

  @Override
  public String toString() {
    return "ConcatenateExpression (\n" +
      "valueA=" + valueA + ",\n" +
      "valueB=" + valueB + "\n" +
    ')';
  }
}
