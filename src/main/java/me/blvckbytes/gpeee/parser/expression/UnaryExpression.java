package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public abstract class UnaryExpression extends AExpression {

  protected final AExpression input;

  @Override
  public String expressionify() {
    return getPrefixSymbol() + " " + input.expressionify();
  }

  protected abstract @Nullable String getPrefixSymbol();
}
