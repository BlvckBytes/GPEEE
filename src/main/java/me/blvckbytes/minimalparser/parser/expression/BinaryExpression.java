package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class BinaryExpression extends AExpression {

  protected final AExpression lhs;
  protected final AExpression rhs;

}
