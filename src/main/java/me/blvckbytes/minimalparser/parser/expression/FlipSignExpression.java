package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class FlipSignExpression extends UnaryExpression {

  public FlipSignExpression(AExpression input) {
    super(input);
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return ((BigDecimal) input.evaluate(context, valueInterpreter)).multiply(new BigDecimal(-1));
  }

  @Override
  protected @Nullable String getPrefixSymbol() {
    return TokenType.MINUS.getRepresentation();
  }
}
