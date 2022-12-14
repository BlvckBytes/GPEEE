package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class StringExpression extends AExpression {

  private final String value;

  public StringExpression(String value, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.value = value;
  }

  @Override
  public String expressionify() {
    return "\"" + value + "\"";
  }
}
