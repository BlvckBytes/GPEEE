package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IntExpression extends AExpression {

  private final Integer number;

  @Override
  public String expressionify() {
    return String.valueOf(number);
  }
}
