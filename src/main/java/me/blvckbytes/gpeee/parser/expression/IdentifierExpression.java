package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class IdentifierExpression extends AExpression {

  private final String symbol;

  public IdentifierExpression(String symbol, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.symbol = symbol;
  }

  @Override
  public String expressionify() {
    return symbol;
  }
}
