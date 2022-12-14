package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.parser.LiteralType;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class LiteralExpression extends AExpression {

  private final LiteralType type;

  public LiteralExpression(LiteralType type, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.type = type;
  }

  @Override
  public String expressionify() {
    return String.valueOf(type.getValue());
  }
}
