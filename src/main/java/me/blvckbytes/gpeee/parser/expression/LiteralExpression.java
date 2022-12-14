package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.gpeee.parser.LiteralType;

@Getter
@AllArgsConstructor
public class LiteralExpression extends AExpression {

  private LiteralType type;

  @Override
  public String expressionify() {
    return String.valueOf(type.getValue());
  }
}
