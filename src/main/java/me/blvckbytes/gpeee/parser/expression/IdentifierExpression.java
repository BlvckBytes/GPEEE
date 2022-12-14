package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdentifierExpression extends AExpression {

  private final String symbol;

  @Override
  public String expressionify() {
    return symbol;
  }
}
