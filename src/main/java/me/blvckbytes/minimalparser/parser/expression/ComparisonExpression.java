package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.parser.ComparisonOperation;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class ComparisonExpression extends BinaryExpression {

  private final ComparisonOperation operation;

  public ComparisonExpression(AExpression lhs, AExpression rhs, ComparisonOperation operation) {
    super(lhs, rhs);
    this.operation = operation;
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    switch (operation) {
      case LESS_THAN:
        return TokenType.LESS_THAN.getRepresentation();
      case GREATER_THAN:
        return TokenType.GREATER_THAN.getRepresentation();
      case LESS_THAN_OR_EQUAL:
        return TokenType.LESS_THAN_OR_EQUAL.getRepresentation();
      case GREATER_THAN_OR_EQUAL:
        return TokenType.GREATER_THAN_OR_EQUAL.getRepresentation();
      default:
        return null;
    }
  }
}
