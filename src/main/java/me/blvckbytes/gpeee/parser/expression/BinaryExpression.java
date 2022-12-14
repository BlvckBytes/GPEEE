package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class BinaryExpression extends AExpression {

  protected final AExpression lhs;
  protected final AExpression rhs;

  public BinaryExpression(AExpression lhs, AExpression rhs, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override
  public String expressionify() {
    return (
      TokenType.PARENTHESIS_OPEN.getRepresentation() +
      lhs.expressionify() + " " + getInfixSymbol() + " " + rhs.expressionify() +
      TokenType.PARENTHESIS_CLOSE.getRepresentation()
    );
  }

  protected abstract @Nullable String getInfixSymbol();
}
