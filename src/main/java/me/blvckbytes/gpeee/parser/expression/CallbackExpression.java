package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.TokenType;

import java.util.List;

@Getter
@AllArgsConstructor
public class CallbackExpression extends AExpression {

  private List<IdentifierExpression> signature;
  private AExpression body;

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
