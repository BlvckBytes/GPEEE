package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

@Getter
@AllArgsConstructor
public class StringExpression extends AExpression {

  private final String value;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return value;
  }
}
