package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class ConjunctionExpression extends BinaryExpression {

  public ConjunctionExpression(AExpression lhs, AExpression rhs) {
    super(lhs, rhs);
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    return TokenType.BOOL_AND.getRepresentation();
  }
}
