package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class ConcatenationExpression extends BinaryExpression {

  public ConcatenationExpression(AExpression lhs, AExpression rhs) {
    super(lhs, rhs);
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return lhs.evaluate(context, valueInterpreter) + String.valueOf(rhs.evaluate(context, valueInterpreter));
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    return TokenType.CONCATENATE.getRepresentation();
  }
}
