package me.blvckbytes.gpeee.parser.expression;

import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class InvertExpression extends UnaryExpression {

  public InvertExpression(AExpression input, Token head, Token tail, String fullContainingExpression) {
    super(input, head, tail, fullContainingExpression);
  }

  @Override
  protected @Nullable String getPrefixSymbol() {
    return TokenType.BOOL_NOT.getRepresentation();
  }
}
