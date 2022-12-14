package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StringExpression extends AExpression {

  private final String value;

  @Override
  public String expressionify() {
    return "\"" + value + "\"";
  }
}
