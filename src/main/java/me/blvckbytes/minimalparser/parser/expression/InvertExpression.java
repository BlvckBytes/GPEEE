package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class InvertExpression extends UnaryExpression {

  public InvertExpression(AExpression input) {
    super(input);
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return !((Boolean) input.evaluate(context, valueInterpreter));
  }

  @Override
  protected @Nullable String getPrefixSymbol() {
    return TokenType.BOOL_NOT.getRepresentation();
  }
}
