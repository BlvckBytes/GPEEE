package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;

import java.util.List;

@Getter
public class FunctionInvocationExpression extends AExpression {

  private final IdentifierExpression name;
  private final List<AExpression> arguments;

  public FunctionInvocationExpression(IdentifierExpression name, List<AExpression> arguments, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.name = name;
    this.arguments = arguments;
  }

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
