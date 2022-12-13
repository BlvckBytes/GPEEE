package me.blvckbytes.minimalparser.parser;

import lombok.AllArgsConstructor;
import me.blvckbytes.minimalparser.ILogger;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.error.UnexpectedTokenError;
import me.blvckbytes.minimalparser.parser.expression.*;
import me.blvckbytes.minimalparser.tokenizer.ITokenizer;
import me.blvckbytes.minimalparser.tokenizer.Token;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

/**
 * This parser uses the compact and flexible algorithm called "precedence climbing" / "top down
 * recursive decent", which of course is not highly efficient. The main purpose of this project is
 * to parse expressions within configuration files once and then just evaluate the AST within the
 * desired evaluation context at runtime over and over again. Due to the ahead-of-time nature of
 * this intended use-case, efficiency at the level of the parser is sacrificed for understandability.
 */
@AllArgsConstructor
public class ExpressionParser {

  private final ILogger logger;
  private final ITokenizer tokenizer;

  /*
    Digit ::= [0-9]
    Letter ::= [A-Za-z]

    Int ::= "-"? Digit+
    Float ::= "-"? Digit* "." Digit+
    String ::= '"' ('\"' | [^"] | "\s")* '"'
    Identifier ::= Letter (Digit | Letter | '_')*
    Literal ::= "true" | "false" | "null"

    AdditiveOperator ::= "+" | "-"
    MultiplicativeOperator ::= "*" | "/" | "%"
    EqualityOperator ::= ">" | "<" | ">=" | "<=" | "==" | "!=" | "===" | "!=="

    PrimaryExpression ::= Int | Float | String | Identifier | Literal

    ExponentiationExpression ::= PrimaryExpression (MultiplicativeOperator PrimaryExpression)*
    MultiplicativeExpression ::= ExponentiationExpression (MultiplicativeOperator ExponentiationExpression)*
    AdditiveExpression ::= MultiplicativeExpression (AdditiveOperator MultiplicativeExpression)*

    MathExpression ::= AdditiveExpression | MultiplicativeExpression | ExponentiationExpression
    EqualityExpression ::= AdditiveExpression (EqualityOperator AdditiveExpression)*

    NegationExpression ::= "not" EqualityExpression

    Expression ::= EqualityExpression | MathExpression | ("-" | "not")? "(" Expression ")" | PrimaryExpression
   */

  /**
   * Main entry point when parsing an expression
   */
  private @Nullable AExpression parseExpression() throws AParserError {
    logger.logDebug("At the main entrypoint of parsing an expression");
    return parseEqualityExpression();
  }

  private AExpression parseNegationExpression() {
    logger.logDebug("Trying to parse a negotiation expression");

    Token tk = tokenizer.peekToken();

    // There's no not operator as the next token, hand over to the next higher precedence parser
    if (tk == null || tk.getType() != TokenType.BOOL_NOT) {
      logger.logDebug("Not a negotiation expression");
      return parseParenthesisExpression();
    }

    // Consume the not operator
    tokenizer.consumeToken();

    // Parse the following expression
    logger.logDebug("Trying to parse the expression to negate");
    AExpression expression = parseParenthesisExpression();

    // Return a wrapper on that expression which will negate it
    logger.logDebug("Wrapping the parsed expression in order to negate it");
    return new InvertExpression(expression);
  }

  private AExpression parseComparisonExpression() {
    logger.logDebug("Trying to parse a comparison expression");

    AExpression lhs = parseAdditiveExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
        (
          tk.getType() == TokenType.GREATER_THAN || tk.getType() == TokenType.GREATER_THAN_OR_EQUAL ||
          tk.getType() == TokenType.LESS_THAN || tk.getType() == TokenType.LESS_THAN_OR_EQUAL
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

        default:
          throw new IllegalStateException();
      }

      // Put the previously parsed expression into the left hand side of the new equality
      // and try to parse another same-precedence expression for the right hand side
      logger.logDebug("Trying to parse a rhs for this comparison expression");
      lhs = new ComparisonExpression(lhs, parseAdditiveExpression(), operator);
    }

    return lhs;
  }

  private AExpression parseEqualityExpression() {
    logger.logDebug("Trying to parse a equality expression");

    AExpression lhs = parseComparisonExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
        (
          tk.getType() == TokenType.VALUE_EQUALS || tk.getType() == TokenType.VALUE_NOT_EQUALS ||
          tk.getType() == TokenType.VALUE_EQUALS_EXACT || tk.getType() == TokenType.VALUE_NOT_EQUALS_EXACT
        )
    ) {
      tokenizer.consumeToken();

      EqualityOperation operator;

      switch (tk.getType()) {
        case VALUE_EQUALS:
          operator = EqualityOperation.EQUAL;
          break;

        case VALUE_NOT_EQUALS:
          operator = EqualityOperation.NOT_EQUAL;
          break;

        case VALUE_EQUALS_EXACT:
          operator = EqualityOperation.EQUAL_EXACT;
          break;

        case VALUE_NOT_EQUALS_EXACT:
          operator = EqualityOperation.NOT_EQUAL_EXACT;
          break;

        default:
          throw new IllegalStateException();
      }

      // Put the previously parsed expression into the left hand side of the new equality
      // and try to parse another same-precedence expression for the right hand side
      logger.logDebug("Trying to parse a rhs for this equality expression");
      lhs = new EqualityExpression(lhs, parseComparisonExpression(), operator);
    }

    return lhs;
  }

  private AExpression parseAdditiveExpression() {
    logger.logDebug("Trying to parse a additive expression");

    AExpression lhs = parseMultiplicativeExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      (tk.getType() == TokenType.PLUS || tk.getType() == TokenType.MINUS)
    ) {
      tokenizer.consumeToken();

      MathOperation operator = MathOperation.ADDITION;

      if (tk.getType() == TokenType.MINUS)
        operator = MathOperation.SUBTRACTION;

      // Put the previously parsed expression into the left hand side of the new addition
      // and try to parse another same-precedence expression for the right hand side
      logger.logDebug("Trying to parse a rhs for this additive operation");
      lhs = new MathExpression(lhs, parseMultiplicativeExpression(), operator);
    }

    return lhs;
  }

  private AExpression parseMultiplicativeExpression() throws AParserError {
    logger.logDebug("Trying to parse a multiplicative expression");

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
      logger.logDebug("Trying to parse a rhs for this multiplicative operation");
      lhs = new MathExpression(lhs, parseExponentiationExpression(), operator);
    }

    return lhs;
  }

  private AExpression parseParenthesisExpression() throws AParserError {
    logger.logDebug("Trying to parse a parenthesis expression");

    Token tk = tokenizer.peekToken();

    // End reached, let the default routine handle this case
    if (tk == null)
      return parsePrimaryExpression();

    TokenType firstTokenType = tk.getType();
    boolean consumedFirstToken = false;

    // The notation -() will flip the resulting number's sign
    // The notation !() will negate the resulting boolean
    if (tk.getType() == TokenType.MINUS || tk.getType() == TokenType.BOOL_NOT) {
      tokenizer.saveState(true);

      tokenizer.consumeToken();
      consumedFirstToken = true;

      logger.logDebug("Found and consumed a parentheses modifier token");

      tk = tokenizer.peekToken();
    }

    // Not a parenthesis expression, let the default routine handle this case
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {

      // Put back the consumed token which would have had an effect on these parentheses
      if (consumedFirstToken) {
        logger.logDebug("Putting the modifier token back");
        tokenizer.restoreState(true);
      }

      logger.logDebug("Not a parenthesis expression");
      return parsePrimaryExpression();
    }

    // Don't need to return anymore as we're now inside parentheses
    if (consumedFirstToken)
      tokenizer.discardState(true);

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    logger.logDebug("Trying to parse the inner expression");

    // Parse the expression within the parentheses
    AExpression expression = parseExpression();

    tk = tokenizer.consumeToken();

    // A previously opened parenthesis has to be closed again
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    logger.logDebug("Validated the closing parenthesis");

    // Wrap the expression within a unary expression based on the first token's type
    switch (firstTokenType) {
      case MINUS:
        logger.logDebug("Wrapping expression in order to flip it's sign");
        expression = new FlipSignExpression(expression);
        break;

      case BOOL_NOT:
        logger.logDebug("Wrapping expression in order to invert it");
        expression = new InvertExpression(expression);
        break;
    }

    return expression;
  }

  private AExpression parseExponentiationExpression() throws AParserError {
    logger.logDebug("Trying to parse a exponentiation expression");

    AExpression lhs = parseNegationExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      tk.getType() == TokenType.EXPONENT
    ) {
      tokenizer.consumeToken();

      // Put the previously parsed expression into the left hand side of the new exponentiation
      // and try to parse another same-precedence expression for the right hand side
      logger.logDebug("Trying to parse a rhs for this exponentiation operation");
      lhs = new MathExpression(lhs, parseNegationExpression(), MathOperation.POWER);
    }

    return lhs;
  }

  private AExpression parsePrimaryExpression() throws AParserError {
    logger.logDebug("Trying to parse a primary expression");

    Token tk = tokenizer.consumeToken();

    if (tk == null)
      throw new UnexpectedTokenError(tokenizer, null, TokenType.valueTypes);

    // Whether the primary expression has been marked as negative
    boolean isNegative = false;

    // Notation of a negative number
    if (tk.getType() == TokenType.MINUS) {
      tk = tokenizer.consumeToken();

      // Either no token left or it's not a number, a float or an identifier
      if (tk == null || !(tk.getType() == TokenType.INT || tk.getType() == TokenType.FLOAT || tk.getType() == TokenType.IDENTIFIER))
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.INT, TokenType.FLOAT, TokenType.IDENTIFIER);

      isNegative = true;
    }

    switch (tk.getType()) {
      case INT:
        logger.logDebug("Found an integer");
        return new IntExpression((isNegative ? -1 : 1) * Integer.parseInt(tk.getValue()));

      case FLOAT:
        logger.logDebug("Found an float");
        return new FloatExpression((isNegative ? -1 : 1) * Float.parseFloat(tk.getValue()));

      case STRING:
        logger.logDebug("Found an string");
        return new StringExpression(tk.getValue());

      case IDENTIFIER:
        logger.logDebug("Found an identifier");
        return new FlipSignExpression(new IdentifierExpression(tk.getValue()));

      case TRUE:
        logger.logDebug("Found the true literal");
        return new LiteralExpression(LiteralType.TRUE);

      case FALSE:
        logger.logDebug("Found the false literal");
      return new LiteralExpression(LiteralType.FALSE);

      case NULL:
        logger.logDebug("Found the null literal");
      return new LiteralExpression(LiteralType.NULL);

      default:
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.valueTypes);
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
      throw new UnexpectedTokenError(tokenizer, tk);

    return result;
  }
}
