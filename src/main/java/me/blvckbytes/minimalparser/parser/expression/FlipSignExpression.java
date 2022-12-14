package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class FlipSignExpression extends UnaryExpression {

  public FlipSignExpression(AExpression input) {
    super(input);
  }

  @Override
  protected @Nullable String getPrefixSymbol() {
    return TokenType.MINUS.getRepresentation();
  }
}
