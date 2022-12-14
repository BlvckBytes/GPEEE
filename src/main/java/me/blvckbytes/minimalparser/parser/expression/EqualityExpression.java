package me.blvckbytes.minimalparser.parser.expression;

import lombok.Getter;
import me.blvckbytes.minimalparser.parser.EqualityOperation;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
public class EqualityExpression extends BinaryExpression {

  private final EqualityOperation operation;

  public EqualityExpression(AExpression lhs, AExpression rhs, EqualityOperation operation) {
    super(lhs, rhs);
    this.operation = operation;
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    switch (operation) {
      case EQUAL:
        return TokenType.VALUE_EQUALS.getRepresentation();
      case NOT_EQUAL:
        return TokenType.VALUE_NOT_EQUALS.getRepresentation();
      case EQUAL_EXACT:
        return TokenType.VALUE_EQUALS_EXACT.getRepresentation();
      case NOT_EQUAL_EXACT:
        return TokenType.VALUE_NOT_EQUALS_EXACT.getRepresentation();
      default:
        return null;
    }
  }
}
