package me.blvckbytes.minimalparser.parser;

import me.blvckbytes.minimalparser.ILogger;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.error.UnexpectedTokenError;
import me.blvckbytes.minimalparser.parser.expression.*;
import me.blvckbytes.minimalparser.tokenizer.ITokenizer;
import me.blvckbytes.minimalparser.tokenizer.Token;
import me.blvckbytes.minimalparser.tokenizer.TokenType;

/**
 * This parser uses the compact and flexible algorithm called "precedence climbing" / "top down
 * recursive decent", which of course is not highly efficient. The main purpose of this project is
 * to parse expressions within configuration files once and then just evaluate the AST within the
 * desired evaluation context at runtime over and over again. Due to the ahead-of-time nature of
 * this intended use-case, efficiency at the level of the parser is sacrificed for understandability.
 */
public class Parser {

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
    EqualityOperator ::= "==" | "!=" | "===" | "!=="
    ComparisonOperator ::= ">" | "<" | ">=" | "<="

    PrimaryExpression ::= Int | Float | String | Identifier | Literal

    NegationExpression ::= "not"? PrimaryExpression

    ExponentiationExpression ::= NegationExpression (MultiplicativeOperator NegationExpression)*
    MultiplicativeExpression ::= ExponentiationExpression (MultiplicativeOperator ExponentiationExpression)*
    AdditiveExpression ::= MultiplicativeExpression (AdditiveOperator MultiplicativeExpression)*

    MathExpression ::= AdditiveExpression | MultiplicativeExpression | ExponentiationExpression
    ComparisonExpression ::= MathExpression (ComparisonOperator MathExpression)*
    EqualityExpression ::= ComparisonExpression (EqualityOperator ComparisonExpression)*

    ConjunctionExpression ::= EqualityExpression ("and" EqualityExpression)*
    DisjunctionExpression ::= ConjunctionExpression ("or" ConjunctionExpression)*
    ConcatenationExpression ::= DisjunctionExpression ("&" DisjunctionExpression)*

    Expression ::= ConcatenationExpression | ("-" | "not")? "(" Expression ")"
   */

  private final ILogger logger;
  private final IExpressionParser[] precedenceLadder;

  public Parser(ILogger logger) {
    this.logger = logger;

    this.precedenceLadder = new IExpressionParser[] {
      this::parseParenthesisExpression,
      this::parseConcatenationExpression,
      this::parseDisjunctionExpression,
      this::parseConjunctionExpression,
      this::parseEqualityExpression,
      this::parseComparisonExpression,
      this::parseAdditiveExpression,
      this::parseMultiplicativeExpression,
      this::parseExponentiationExpression,
      this::parseNegationExpression,
      this::parsePrimaryExpression,
    };
  }

  /**
   * Parses all available tokens into an abstract syntax tree (AST)
   * @return AST root ready for execution
   */
  public AExpression parse(ITokenizer tokenizer) throws AParserError {
    // Start to parse the lowest precedence expression and climb up
    AExpression result = precedenceLadder[0].apply(tokenizer, precedenceLadder, 0);

    // If there are still tokens left after parsing an expression, the expression
    // wasn't closed in itself and has thus to be malformed, as this parser is only
    // intended for mono-expression "programs"
    Token tk = tokenizer.peekToken();
    if (tk != null)
      throw new UnexpectedTokenError(tokenizer, tk);

    return result;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Invokes the next expression parser within the sequence dictated by the precedence ladder
   * @param tokenizer Current parsing context's tokenizer reference
   * @param precedenceLadder Precedence ladder
   * @param precedenceSelf Precedence of the current expression parser wanting to invoke the next
   * @return Result of invoking the next expression parser
   */
  private AExpression invokeNextPrecedenceParser(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    return precedenceLadder[precedenceSelf + 1].apply(tokenizer, precedenceLadder, precedenceSelf + 1);
  }

  //=========================================================================//
  //                            Expression Parsers                           //
  //=========================================================================//

  private AExpression parseNegationExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a negotiation expression");

    Token tk = tokenizer.peekToken();

    // There's no not operator as the next token, hand over to the next higher precedence parser
    if (tk == null || tk.getType() != TokenType.BOOL_NOT) {
      logger.logDebug("Not a negotiation expression");
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    // Consume the not operator
    tokenizer.consumeToken();

    // Parse the following expression
    logger.logDebug("Trying to parse the expression to negate");
    AExpression expression = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);

    // Return a wrapper on that expression which will negate it
    logger.logDebug("Wrapping the parsed expression in order to negate it");
    return new InvertExpression(expression);
  }

  private AExpression parseComparisonExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a comparison expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
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

      logger.logDebug("Trying to parse a rhs for this comparison expression");
      lhs = new ComparisonExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseConcatenationExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a concatenation expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk;

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.CONCATENATE) {
      tokenizer.consumeToken();

      logger.logDebug("Trying to parse a rhs for this concatenation expression");
      lhs = new ConcatenationExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf));
    }

    return lhs;
  }

  private AExpression parseDisjunctionExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a disjunction expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk;

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.BOOL_OR) {
      tokenizer.consumeToken();

      logger.logDebug("Trying to parse a rhs for this disjunction expression");
      lhs = new DisjunctionExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf));
    }

    return lhs;
  }

  private AExpression parseConjunctionExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a conjunction expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer,  precedenceLadder, precedenceSelf);
    Token tk;

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.BOOL_AND) {
      tokenizer.consumeToken();

      logger.logDebug("Trying to parse a rhs for this conjunction expression");
      lhs = new ConjunctionExpression(lhs, invokeNextPrecedenceParser(tokenizer,  precedenceLadder, precedenceSelf));
    }

    return lhs;
  }

  private AExpression parseEqualityExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a equality expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
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

      logger.logDebug("Trying to parse a rhs for this equality expression");
      lhs = new EqualityExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseAdditiveExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a additive expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      (tk.getType() == TokenType.PLUS || tk.getType() == TokenType.MINUS)
    ) {
      tokenizer.consumeToken();

      MathOperation operator = MathOperation.ADDITION;

      if (tk.getType() == TokenType.MINUS)
        operator = MathOperation.SUBTRACTION;

      logger.logDebug("Trying to parse a rhs for this additive operation");
      lhs = new MathExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseMultiplicativeExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a multiplicative expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
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

      logger.logDebug("Trying to parse a rhs for this multiplicative operation");
      lhs = new MathExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseParenthesisExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a parenthesis expression");

    Token tk = tokenizer.peekToken();

    // End reached, let the default routine handle this case
    if (tk == null)
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);

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
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    // Don't need to return anymore as we're now inside parentheses
    if (consumedFirstToken)
      tokenizer.discardState(true);

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    logger.logDebug("Trying to parse the inner expression");

    // Parse the expression within the parentheses (start climbing the ladder all over again)
    AExpression expression = precedenceLadder[0].apply(tokenizer, precedenceLadder, 0);

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

  private AExpression parseExponentiationExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    logger.logDebug("Trying to parse a exponentiation expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      tk.getType() == TokenType.EXPONENT
    ) {
      tokenizer.consumeToken();

      logger.logDebug("Trying to parse a rhs for this exponentiation operation");
      lhs = new MathExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), MathOperation.POWER);
    }

    return lhs;
  }

  private AExpression parsePrimaryExpression(ITokenizer tokenizer, IExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
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
}
