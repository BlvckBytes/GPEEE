package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class FloatExpression extends AExpression {

  private final Float value;

  public FloatExpression(Float value, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.value = value;
  }

  @Override
  public String expressionify() {
    return String.valueOf(value);
  }
}
