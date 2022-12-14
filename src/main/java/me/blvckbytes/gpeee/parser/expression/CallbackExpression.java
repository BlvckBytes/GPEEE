package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;

import java.util.List;

@Getter
public class CallbackExpression extends AExpression {

  private final List<IdentifierExpression> signature;
  private final AExpression body;

  public CallbackExpression(List<IdentifierExpression> signature, AExpression body, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.signature = signature;
    this.body = body;
  }

  @Override
  public String expressionify() {
    StringBuilder argExpression = new StringBuilder();

    for (int i = 0; i < signature.size(); i++) {
      IdentifierExpression argument = signature.get(i);
      argExpression.append(i == 0 ? "" : ", ").append(argument.getSymbol());
    }

    return (
      TokenType.PARENTHESIS_OPEN.getRepresentation() +
      argExpression +
      TokenType.PARENTHESIS_CLOSE.getRepresentation() +
      " " + TokenType.ARROW.getRepresentation() + " " +
      body.expressionify()
    );
  }
}
