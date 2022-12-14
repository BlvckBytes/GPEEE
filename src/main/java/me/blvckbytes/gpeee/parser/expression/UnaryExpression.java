package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class UnaryExpression extends AExpression {

  protected final AExpression input;

  public UnaryExpression(AExpression input, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.input = input;
  }

  @Override
  public String expressionify() {
    return getPrefixSymbol() + " " + input.expressionify();
  }

  protected abstract @Nullable String getPrefixSymbol();
}
