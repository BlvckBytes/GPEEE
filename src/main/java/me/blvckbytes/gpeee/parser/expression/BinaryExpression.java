package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public abstract class BinaryExpression extends AExpression {

  protected final AExpression lhs;
  protected final AExpression rhs;

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
