package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class DoubleExpression extends AExpression {

  private final ExpressionValue value;

  public DoubleExpression(Double value, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.value = ExpressionValue.fromDouble(value);
  }

  @Override
  public String expressionify() {
    return String.valueOf(value);
  }
}
