package me.blvckbytes.gpeee.parser;

import me.blvckbytes.gpeee.error.AParserError;
import me.blvckbytes.gpeee.error.UnexpectedTokenError;
import me.blvckbytes.gpeee.parser.expression.*;
import me.blvckbytes.gpeee.tokenizer.ITokenizer;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This parser uses the compact and flexible algorithm called "precedence climbing" / "top down
 * recursive decent", which of course is not highly efficient. The main purpose of this project is
 * to parse expressions within configuration files once and then just evaluate the AST within the
 * desired evaluation context at runtime over and over again. Due to the ahead-of-time nature of
 * this intended use-case, efficiency at the level of the parser is sacrificed for understandability.
 */
public class Parser {

  private final Consumer<String> debugLogger;
  private final FExpressionParser[] precedenceLadder;

  public Parser(Consumer<String> debugLogger) {
    this.debugLogger = debugLogger;

    this.precedenceLadder = new FExpressionParser[] {
      this::parseConcatenationExpression,
      this::parseDisjunctionExpression,
      this::parseConjunctionExpression,
      this::parseEqualityExpression,
      this::parseComparisonExpression,
      this::parseAdditiveExpression,
      this::parseMultiplicativeExpression,
      this::parseExponentiationExpression,
      this::parseNegationExpression,
      this::parseFunctionInvocationExpression,
      this::parseCallbackExpression,
      this::parseParenthesisExpression,
      (tk, l, s) -> this.parsePrimaryExpression(tk),
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
  private AExpression invokeNextPrecedenceParser(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    return precedenceLadder[precedenceSelf + 1].apply(tokenizer, precedenceLadder, precedenceSelf + 1);
  }

  //=========================================================================//
  //                            Expression Parsers                           //
  //=========================================================================//

  private AExpression parseCallbackExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a callback expression");

    Token tk = tokenizer.peekToken();

    // There's no opening parenthesis as the next token, hand over to the next higher precedence parser
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {
      debugLogger.accept("Not a callback expression");
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    // Save once before consuming anything
    tokenizer.saveState(true);

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    List<IdentifierExpression> signature = new ArrayList<>();

    // As long as there is no closing parenthesis, there are still arguments left
    while ((tk = tokenizer.peekToken()) != null && tk.getType() != TokenType.PARENTHESIS_CLOSE) {
      debugLogger.accept("Parsing argument " + signature.size());

      if (signature.size() > 0) {
        // Arguments other than the first one need to be separated out by a comma
        if (tk.getType() != TokenType.COMMA)
          throw new UnexpectedTokenError(tokenizer, tk, TokenType.COMMA);

        // Consume that comma
        tokenizer.consumeToken();
      }

      AExpression identifier = parsePrimaryExpression(tokenizer);

      // Anything else than an identifier cannot be within a callback's parentheses
      if (!(identifier instanceof IdentifierExpression)) {
        debugLogger.accept("Not a callback expression");
        tokenizer.restoreState(true);
        return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      }

      signature.add((IdentifierExpression) identifier);
    }

    // Callback signatures have to be terminated with a closing parenthesis
    tk = tokenizer.consumeToken();
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    // Expect and consume the arrow operator
    tk = tokenizer.consumeToken();
    if (tk == null || tk.getType() != TokenType.ARROW)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.ARROW);

    // Parse the callback body expression (start climbing the ladder all over again)
    AExpression body = precedenceLadder[0].apply(tokenizer, precedenceLadder, 0);

    return new CallbackExpression(signature, body);
  }

  private AExpression parseFunctionInvocationExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a function invocation expression");

    Token tk = tokenizer.peekToken();

    // There's no identifier or minus as the next token, hand over to the next higher precedence parser
    if (tk == null || !(tk.getType() == TokenType.IDENTIFIER || tk.getType() == TokenType.MINUS)) {
      debugLogger.accept("Not a function invocation expression");
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    boolean flipResult = false;

    // The function return value's sign should be flipped
    if (tk.getType() == TokenType.MINUS) {
      flipResult = true;

      // Store before consuming the minus
      tokenizer.saveState(true);
      tokenizer.consumeToken();

      tk = tokenizer.peekToken();

      // There's no identifier as the next token, hand over to the next higher precedence parser
      if (tk == null || tk.getType() != TokenType.IDENTIFIER) {
        debugLogger.accept("Not a function invocation expression");

        // Put back the minus
        tokenizer.restoreState(true);

        return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      }
    }

    // Store before consuming the identifier
    tokenizer.saveState(true);

    // Consume the identifier
    Token identifier = tk;
    tokenizer.consumeToken();

    tk = tokenizer.peekToken();

    // There's no opening parenthesis as the next token, hand over to the next higher precedence parser
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {
      debugLogger.accept("Not a function invocation expression");

      // Put back the token
      tokenizer.restoreState(true);

      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    // Not going to go need to restore anymore, this has to be a function invocation
    tokenizer.discardState(true);

    // Also discard the save from parsing the minus
    if (flipResult)
      tokenizer.discardState(true);

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    List<AExpression> arguments = new ArrayList<>();

    // As long as there is no closing parenthesis, there are still arguments left
    while ((tk = tokenizer.peekToken()) != null && tk.getType() != TokenType.PARENTHESIS_CLOSE) {
      debugLogger.accept("Parsing argument " + arguments.size());

      if (arguments.size() > 0) {
        // Arguments other than the first one need to be separated out by a comma
        if (tk.getType() != TokenType.COMMA)
          throw new UnexpectedTokenError(tokenizer, tk, TokenType.COMMA);

        // Consume that comma
        tokenizer.consumeToken();
      }

      // Parse the argument expression (start climbing the ladder all over again)
      arguments.add(precedenceLadder[0].apply(tokenizer, precedenceLadder, 0));
    }

    // Function invocations have to be terminated with a closing parenthesis
    tk = tokenizer.consumeToken();
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    FunctionInvocationExpression functionExpression = new FunctionInvocationExpression(new IdentifierExpression(identifier.getValue()), arguments);
    return flipResult ? new FlipSignExpression(functionExpression) : functionExpression;
  }

  private AExpression parseNegationExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a negotiation expression");

    Token tk = tokenizer.peekToken();

    // There's no not operator as the next token, hand over to the next higher precedence parser
    if (tk == null || tk.getType() != TokenType.BOOL_NOT) {
      debugLogger.accept("Not a negotiation expression");
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    // Consume the not operator
    tokenizer.consumeToken();

    // Parse the following expression
    debugLogger.accept("Trying to parse the expression to negate");
    AExpression expression = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);

    // Return a wrapper on that expression which will negate it
    debugLogger.accept("Wrapping the parsed expression in order to negate it");
    return new InvertExpression(expression);
  }

  private AExpression parseComparisonExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a comparison expression");

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

      debugLogger.accept("Trying to parse a rhs for this comparison expression");
      lhs = new ComparisonExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseConcatenationExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a concatenation expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk;

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.CONCATENATE) {
      tokenizer.consumeToken();

      debugLogger.accept("Trying to parse a rhs for this concatenation expression");
      lhs = new ConcatenationExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf));
    }

    return lhs;
  }

  private AExpression parseDisjunctionExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a disjunction expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk;

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.BOOL_OR) {
      tokenizer.consumeToken();

      debugLogger.accept("Trying to parse a rhs for this disjunction expression");
      lhs = new DisjunctionExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf));
    }

    return lhs;
  }

  private AExpression parseConjunctionExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a conjunction expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer,  precedenceLadder, precedenceSelf);
    Token tk;

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.BOOL_AND) {
      tokenizer.consumeToken();

      debugLogger.accept("Trying to parse a rhs for this conjunction expression");
      lhs = new ConjunctionExpression(lhs, invokeNextPrecedenceParser(tokenizer,  precedenceLadder, precedenceSelf));
    }

    return lhs;
  }

  private AExpression parseEqualityExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a equality expression");

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

      debugLogger.accept("Trying to parse a rhs for this equality expression");
      lhs = new EqualityExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseAdditiveExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a additive expression");

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

      debugLogger.accept("Trying to parse a rhs for this additive operation");
      lhs = new MathExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseMultiplicativeExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a multiplicative expression");

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

      debugLogger.accept("Trying to parse a rhs for this multiplicative operation");
      lhs = new MathExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), operator);
    }

    return lhs;
  }

  private AExpression parseParenthesisExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a parenthesis expression");

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

      debugLogger.accept("Found and consumed a parentheses modifier token");

      tk = tokenizer.peekToken();
    }

    // Not a parenthesis expression, let the default routine handle this case
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {

      // Put back the consumed token which would have had an effect on these parentheses
      if (consumedFirstToken) {
        debugLogger.accept("Putting the modifier token back");
        tokenizer.restoreState(true);
      }

      debugLogger.accept("Not a parenthesis expression");
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    // Don't need to return anymore as we're now inside parentheses
    if (consumedFirstToken)
      tokenizer.discardState(true);

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    debugLogger.accept("Trying to parse the inner expression");

    // Parse the expression within the parentheses (start climbing the ladder all over again)
    AExpression expression = precedenceLadder[0].apply(tokenizer, precedenceLadder, 0);

    tk = tokenizer.consumeToken();

    // A previously opened parenthesis has to be closed again
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    debugLogger.accept("Validated the closing parenthesis");

    // Wrap the expression within a unary expression based on the first token's type
    switch (firstTokenType) {
      case MINUS:
        debugLogger.accept("Wrapping expression in order to flip it's sign");
        expression = new FlipSignExpression(expression);
        break;

      case BOOL_NOT:
        debugLogger.accept("Wrapping expression in order to invert it");
        expression = new InvertExpression(expression);
        break;
    }

    return expression;
  }

  private AExpression parseExponentiationExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AParserError {
    debugLogger.accept("Trying to parse a exponentiation expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      tk.getType() == TokenType.EXPONENT
    ) {
      tokenizer.consumeToken();

      debugLogger.accept("Trying to parse a rhs for this exponentiation operation");
      lhs = new MathExpression(lhs, invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf), MathOperation.POWER);
    }

    return lhs;
  }

  private AExpression parsePrimaryExpression(ITokenizer tokenizer) throws AParserError {
    debugLogger.accept("Trying to parse a primary expression");

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
        debugLogger.accept("Found an integer");
        return new IntExpression((isNegative ? -1 : 1) * Integer.parseInt(tk.getValue()));

      case FLOAT:
        debugLogger.accept("Found a float");
        return new FloatExpression((isNegative ? -1 : 1) * Float.parseFloat(tk.getValue()));

      case STRING:
        debugLogger.accept("Found a string");
        return new StringExpression(tk.getValue());

      case IDENTIFIER: {
        debugLogger.accept("Found an identifier");
        IdentifierExpression identifier = new IdentifierExpression(tk.getValue());
        return isNegative ? new FlipSignExpression(identifier) : identifier;
      }

      case TRUE:
        debugLogger.accept("Found the true literal");
        return new LiteralExpression(LiteralType.TRUE);

      case FALSE:
        debugLogger.accept("Found the false literal");
      return new LiteralExpression(LiteralType.FALSE);

      case NULL:
        debugLogger.accept("Found the null literal");
      return new LiteralExpression(LiteralType.NULL);

      default:
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.valueTypes);
    }
  }
}
