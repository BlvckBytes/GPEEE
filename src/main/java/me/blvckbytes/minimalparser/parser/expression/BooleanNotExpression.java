package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

@Getter
@AllArgsConstructor
public class BooleanNotExpression extends AExpression {

  private final Object value;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return !valueInterpreter.isTruthy(evaluateExpression(value, context, valueInterpreter));
  }

  @Override
  public String toString() {
    return "BooleanNotExpression (\n" +
      "value=" + value + ",\n" +
    ')';
  }
}
