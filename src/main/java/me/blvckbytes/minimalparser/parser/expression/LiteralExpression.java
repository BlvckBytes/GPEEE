package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.LiteralType;

@AllArgsConstructor
public class LiteralExpression extends AExpression {

  private LiteralType type;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return type.getValue();
  }

  @Override
  public String expressionify() {
    return String.valueOf(type.getValue());
  }
}
