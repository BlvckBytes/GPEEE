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
    EqualityOperator ::= ">" | "<" | ">=" | "<=" | "==" | "!=" | "===" | "!=="

    PrimaryExpression ::= Int | Float | String | Identifier
    ExponentiationExpression ::= PrimaryExpression (MultiplicativeOperator PrimaryExpression)*
    MultiplicativeExpression ::= ExponentiationExpression (MultiplicativeOperator ExponentiationExpression)*
    AdditiveExpression ::= MultiplicativeExpression (AdditiveOperator MultiplicativeExpression)*

    MathExpression ::= AdditiveExpression | MultiplicativeExpression | ExponentiationExpression

    EqualityExpression ::= AdditiveExpression (EqualityOperator AdditiveExpression)*
   */

  /**
   * Main entry point when parsing an expression
   */
  private @Nullable AExpression parseExpression() throws AParserError {
    return parseEqualityExpression();
  }

  /**
   * Parses an expression made up of equality expressions with additive expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no equality operator available, this path will yield a additive expression.
   */
  private AExpression parseEqualityExpression() {
    AExpression lhs = parseAdditiveExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
        (
          tk.getType() == TokenType.GREATER_THAN || tk.getType() == TokenType.GREATER_THAN_OR_EQUAL ||
          tk.getType() == TokenType.LESS_THAN || tk.getType() == TokenType.LESS_THAN_OR_EQUAL ||
          tk.getType() == TokenType.VALUE_EQUALS || tk.getType() == TokenType.VALUE_NOT_EQUALS ||
          tk.getType() == TokenType.VALUE_EQUALS_EXACT || tk.getType() == TokenType.VALUE_NOT_EQUALS_EXACT
        )
    ) {
      tokenizer.consumeToken();

      ComparisonOperation operator;

      switch (tk.getType()) {
        case GREATER_THAN:
          operator = ComparisonOperation.GREATER_THAN;
          break;

        case GREATER_THAN_OR_EQUAL:
          operator = ComparisonOperation.GREATER_THAN_OR_EQUAL;
          break;

        case LESS_THAN:
          operator = ComparisonOperation.LESS_THAN;
          break;

        case LESS_THAN_OR_EQUAL:
          operator = ComparisonOperation.LESS_THAN_OR_EQUAL;
          break;

        case VALUE_EQUALS:
          operator = ComparisonOperation.EQUAL;
          break;

        case VALUE_EQUALS_EXACT:
          operator = ComparisonOperation.EQUAL_EXACT;
          break;

        case VALUE_NOT_EQUALS:
          operator = ComparisonOperation.NOT_EQUAL;
          break;

        case VALUE_NOT_EQUALS_EXACT:
          operator = ComparisonOperation.NOT_EQUAL_EXACT;
          break;

        default:
          throw new IllegalStateException();
      }

      // Put the previously parsed expression into the left hand side of the new equality
      // and try to parse another same-precedence expression for the right hand side
      lhs = new ComparisonExpression(lhs, parseAdditiveExpression(), operator);
    }

    return lhs;
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

      // Put the previously parsed expression into the left hand side of the new addition
      // and try to parse another same-precedence expression for the right hand side
      lhs = new MathExpression(lhs, parseMultiplicativeExpression(), operator);
    }

    return lhs;
  }

  /**
   * Parses an expression made up of multiplicative expressions with exponentiation expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no multiplicative operator available, this path will yield a exponentiation expression.
   */
  private AExpression parseMultiplicativeExpression() throws AParserError {
    AExpression lhs = parseExponentiationExpression();
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

      // Put the previously parsed expression into the left hand side of the new multiplication
      // and try to parse another same-precedence expression for the right hand side
      lhs = new MathExpression(lhs, parseExponentiationExpression(), operator);
    }

    return lhs;
  }

  /**
   * Parses an expression made up of exponential expressions with primary expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no exponentiation operator available, this path will yield a primary expression.
   */
  private AExpression parseExponentiationExpression() throws AParserError {
    AExpression lhs = parsePrimaryExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      tk.getType() == TokenType.EXPONENT
    ) {
      tokenizer.consumeToken();

      // Put the previously parsed expression into the left hand side of the new exponentiation
      // and try to parse another same-precedence expression for the right hand side
      lhs = new MathExpression(lhs, parsePrimaryExpression(), MathOperation.POWER);
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
