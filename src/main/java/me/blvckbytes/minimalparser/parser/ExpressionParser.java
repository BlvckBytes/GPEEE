package me.blvckbytes.minimalparser.parser;

import lombok.AllArgsConstructor;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.error.UnexpectedTokenError;
import me.blvckbytes.minimalparser.parser.expression.*;
import me.blvckbytes.minimalparser.tokenizer.ITokenizer;
import me.blvckbytes.minimalparser.tokenizer.Token;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class ExpressionParser {

  private final ITokenizer tokenizer;

  /*
    AdditiveOperator ::= "+" | "-"
    MultiplicativeOperator ::= "*" | "/" | "%"

    PrimaryExpression ::= Int | Float | String | Identifier
    MultiplicativeExpression ::= PrimaryExpression (MultiplicativeOperator PrimaryExpression)*
    AdditiveExpression ::= MultiplicativeExpression (AdditiveOperator MultiplicativeExpression)*
   */

  /**
   * Main entry point when parsing an expression
   */
  private @Nullable AExpression parseExpression() throws AParserError {
    return parseAdditiveExpression();
  }

  /**
   * Parses an expression made up of additive expressions with multiplicative expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no additive operator available, this path will yield a multiplicative expression.
   */
  private AExpression parseAdditiveExpression() {
    AExpression lhs = parseMultiplicativeExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      (tk.getType() == TokenType.ADDITION || tk.getType() == TokenType.SUBTRACTION)
    ) {
      tokenizer.consumeToken();

      MathOperation operator = MathOperation.ADDITION;

      if (tk.getType() == TokenType.SUBTRACTION)
        operator = MathOperation.SUBTRACTION;

      lhs = new BinaryExpression(lhs, parseMultiplicativeExpression(), operator);
    }

    return lhs;
  }

  /**
   * Parses an expression made up of multiplicative expressions with primary expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no multiplicative operator available, this path will yield a primary expression.
   */
  private AExpression parseMultiplicativeExpression() throws AParserError {
    AExpression lhs = parsePrimaryExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      (tk.getType() == TokenType.MULTIPLICATION || tk.getType() == TokenType.DIVISION || tk.getType() == TokenType.MODULO)
    ) {
      tokenizer.consumeToken();

      MathOperation operator = MathOperation.MULTIPLICATION;

      if (tk.getType() == TokenType.DIVISION)
        operator = MathOperation.DIVISION;

      else if (tk.getType() == TokenType.MODULO)
        operator = MathOperation.MODULO;

      lhs = new BinaryExpression(lhs, parsePrimaryExpression(), operator);
    }

    return lhs;
  }

  /**
   * Parses a primary expression - the smallest still meaningful expression possible which
   * then can be intertwined with all other available expressions
   * @throws AParserError No token available to consume or the token type mismatched
   */
  private AExpression parsePrimaryExpression() throws AParserError {
    Token tk = tokenizer.consumeToken();

    if (tk == null)
      throw new UnexpectedTokenError(tokenizer.getCurrentRow(), tokenizer.getCurrentCol(), null, TokenType.valueTypes);

    switch (tk.getType()) {
      case INT:
        return new IntExpression(Integer.parseInt(tk.getValue()));

      case FLOAT:
        return new FloatExpression(Float.parseFloat(tk.getValue()));

      case STRING:
        return new StringExpression(tk.getValue());

      case IDENTIFIER:
        return new IdentifierExpression(tk.getValue());

      default:
        throw new UnexpectedTokenError(tk.getRow(), tk.getCol(), tk.getType(), TokenType.valueTypes);
    }
  }

  /**
   * Parses the noted expression into an abstract syntax tree
   * @return AST root ready for execution
   */
  public AExpression parse() throws AParserError {
    AExpression result = parseExpression();
    Token tk = tokenizer.peekToken();

    // If there are still tokens left after parsing an expression, the expression
    // wasn't closed in itself and has thus to be malformed, as this parser is only
    // intended for mono-expression "programs"
    if (tk != null)
      throw new UnexpectedTokenError(tk.getRow(), tk.getCol(), tk.getType());

    return result;
  }
}
