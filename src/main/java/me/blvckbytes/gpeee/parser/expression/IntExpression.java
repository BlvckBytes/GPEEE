package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class IntExpression extends AExpression {

  private final ExpressionValue number;

  public IntExpression(Integer number, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.number = ExpressionValue.fromInteger(number);
  }

  @Override
  public String expressionify() {
    return String.valueOf(number);
  }
}
