package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.parser.MathOperation;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
public class MathExpression extends BinaryExpression {

  private final MathOperation operation;

  public MathExpression(AExpression lhs, AExpression rhs, MathOperation operation, Token head, Token tail, String fullContainingExpression) {
    super(lhs, rhs, head, tail, fullContainingExpression);
    this.operation = operation;
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    switch (operation) {
      case ADDITION:
        return TokenType.PLUS.getRepresentation();
      case SUBTRACTION:
        return TokenType.MINUS.getRepresentation();
      case MULTIPLICATION:
        return TokenType.MULTIPLICATION.getRepresentation();
      case DIVISION:
        return TokenType.DIVISION.getRepresentation();
      case MODULO:
        return TokenType.MODULO.getRepresentation();
      case POWER:
        return TokenType.EXPONENT.getRepresentation();
      default:
        return null;
    }
  }
}
