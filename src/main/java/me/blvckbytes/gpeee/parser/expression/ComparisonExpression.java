package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.parser.ComparisonOperation;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
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
