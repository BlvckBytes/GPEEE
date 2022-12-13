package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

public class InvertExpression extends UnaryExpression {

  public InvertExpression(AExpression input) {
    super(input);
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return !((Boolean) input.evaluate(context, valueInterpreter));
  }
}
