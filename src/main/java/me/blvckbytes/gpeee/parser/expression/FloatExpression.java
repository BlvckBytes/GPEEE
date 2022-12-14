package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FloatExpression extends AExpression {

  private final Float value;

  @Override
  public String expressionify() {
    return String.valueOf(value);
  }
}
