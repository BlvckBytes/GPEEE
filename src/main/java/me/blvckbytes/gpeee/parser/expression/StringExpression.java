package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class StringExpression extends AExpression {

  private final ExpressionValue value;

  public StringExpression(String value, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.value = ExpressionValue.fromString(value);
  }

  @Override
  public String expressionify() {
    return "\"" + value + "\"";
  }
}
