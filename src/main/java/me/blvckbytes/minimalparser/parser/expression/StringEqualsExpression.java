package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

@Getter
@AllArgsConstructor
public class StringEqualsExpression extends AExpression {

  private final Object valueA, valueB;
  private final boolean exact;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    String stringA = evaluateExpression(valueA, context, valueInterpreter).toString();
    String stringB = evaluateExpression(valueB, context, valueInterpreter).toString();

    if (exact)
      return stringA.equals(stringB);

    return stringA.equalsIgnoreCase(stringB);
  }
}
