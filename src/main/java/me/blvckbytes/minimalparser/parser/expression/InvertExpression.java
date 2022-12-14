package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class InvertExpression extends UnaryExpression {

  public InvertExpression(AExpression input) {
    super(input);
  }

  @Override
  protected @Nullable String getPrefixSymbol() {
    return TokenType.BOOL_NOT.getRepresentation();
  }
}
