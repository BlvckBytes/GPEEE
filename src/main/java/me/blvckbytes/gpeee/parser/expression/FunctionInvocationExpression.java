package me.blvckbytes.gpeee.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.TokenType;

import java.util.List;

@Getter
@AllArgsConstructor
public class FunctionInvocationExpression extends AExpression {

  private IdentifierExpression name;
  private List<AExpression> arguments;

  @Override
  public String expressionify() {
    StringBuilder argExpression = new StringBuilder();

    for (int i = 0; i < arguments.size(); i++) {
      AExpression argument = arguments.get(i);
      argExpression.append(i == 0 ? "" : ", ").append(argument.expressionify());
    }

    return (
      name.expressionify() + TokenType.PARENTHESIS_OPEN.getRepresentation() +
      argExpression +
      TokenType.PARENTHESIS_CLOSE.getRepresentation()
    );
  }
}
