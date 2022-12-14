package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.interpreter.ExpressionValue;
import me.blvckbytes.gpeee.parser.LiteralType;
import me.blvckbytes.gpeee.tokenizer.Token;

@Getter
public class LiteralExpression extends AExpression {

  private final LiteralType type;
  private final ExpressionValue value;

  public LiteralExpression(LiteralType type, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.type = type;

    switch (type) {
      case NULL:
        this.value = ExpressionValue.fromNull();
        break;

      default:
      case TRUE:
      case FALSE:
        this.value = ExpressionValue.fromBoolean(type == LiteralType.TRUE);
        break;
    }
  }

  @Override
  public String expressionify() {
    return String.valueOf(type.getValue());
  }
}
