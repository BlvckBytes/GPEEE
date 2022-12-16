/*
 * MIT License
 *
 * Copyright (c) 2022 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.gpeee.parser;

import me.blvckbytes.gpeee.logging.DebugLogLevel;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.error.UnexpectedTokenError;
import me.blvckbytes.gpeee.parser.expression.*;
import me.blvckbytes.gpeee.tokenizer.ITokenizer;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This parser uses the compact and flexible algorithm called "precedence climbing" / "top down
 * recursive decent", which of course is not highly efficient. The main purpose of this project is
 * to parse expressions within configuration files once and then just evaluate the AST within the
 * desired evaluation context at runtime over and over again. Due to the ahead-of-time nature of
 * this intended use-case, efficiency at the level of the parser is sacrificed for understandability.
 */
public class Parser {

  private final ILogger logger;
  private final FExpressionParser[] precedenceLadder;

  public Parser(ILogger logger) {
    this.logger = logger;

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
      this::parseExpression,
      (tk, l, s) -> this.parsePrimaryExpression(tk),
    };
  }

  /**
   * Parses all available tokens into an abstract syntax tree (AST)
   * @return AST root ready for execution
   */
  public AExpression parse(ITokenizer tokenizer) throws AEvaluatorError {
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
  private AExpression invokeNextPrecedenceParser(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    return precedenceLadder[precedenceSelf + 1].apply(tokenizer, precedenceLadder, precedenceSelf + 1);
  }

  //=========================================================================//
  //                            Expression Parsers                           //
  //=========================================================================//

  private @Nullable AExpression parseCallbackExpression(ITokenizer tokenizer) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a callback expression");

    Token tk = tokenizer.peekToken();

    // There's no opening parenthesis as the next token
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {
      logger.logDebug( DebugLogLevel.PARSER, "Not a callback expression");
      return null;
    }

    // Save once before consuming anything
    tokenizer.saveState(true);

    // Consume the opening parenthesis
    Token head = tokenizer.consumeToken();

    List<IdentifierExpression> signature = new ArrayList<>();

    // As long as there is no closing parenthesis, there are still arguments left
    while ((tk = tokenizer.peekToken()) != null && tk.getType() != TokenType.PARENTHESIS_CLOSE) {
      logger.logDebug( DebugLogLevel.PARSER, "Parsing argument " + signature.size());

      if (signature.size() > 0) {
        // Arguments other than the first one need to be separated out by a comma
        // If there's no comma, this cannot be a callback expression and is more likely a parenthesis expression
        if (tk.getType() != TokenType.COMMA) {
          logger.logDebug( DebugLogLevel.PARSER, "Not a callback expression");
          tokenizer.restoreState(true);
          return null;
        }

        // Consume that comma
        tokenizer.consumeToken();
      }

      AExpression identifier = parsePrimaryExpression(tokenizer);

      // Anything else than an identifier cannot be within a callback's parentheses
      if (!(identifier instanceof IdentifierExpression)) {
        logger.logDebug( DebugLogLevel.PARSER, "Not a callback expression");
        tokenizer.restoreState(true);
        return null;
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

    return new CallbackExpression(signature, body, head, body.getTail(), tokenizer.getRawText());
  }

  private @Nullable AExpression parseFunctionInvocationExpression(ITokenizer tokenizer) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a function invocation expression");

    Token tk = tokenizer.peekToken();

    // There's no identifier or minus as the next token
    if (tk == null || !(tk.getType() == TokenType.IDENTIFIER || tk.getType() == TokenType.MINUS)) {
      logger.logDebug( DebugLogLevel.PARSER, "Not a function invocation expression");
      return null;
    }

    boolean flipResult = false;
    Token tokenMinus = null;

    // The function return value's sign should be flipped
    if (tk.getType() == TokenType.MINUS) {
      flipResult = true;

      // Store before consuming the minus
      tokenizer.saveState(true);
      tokenMinus = tokenizer.consumeToken();

      tk = tokenizer.peekToken();

      // There's no identifier as the next token, hand over to the next higher precedence parser
      if (tk == null || tk.getType() != TokenType.IDENTIFIER) {
        logger.logDebug( DebugLogLevel.PARSER, "Not a function invocation expression");

        // Put back the minus
        tokenizer.restoreState(true);
        return null;
      }
    }

    // Store before consuming the identifier
    tokenizer.saveState(true);

    // Consume the identifier
    Token tokenIdentifier = tk;
    tokenizer.consumeToken();

    tk = tokenizer.peekToken();

    // There's no opening parenthesis as the next token
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {
      logger.logDebug( DebugLogLevel.PARSER, "Not a function invocation expression");

      // Put back the token
      tokenizer.restoreState(true);
      return null;
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
      logger.logDebug( DebugLogLevel.PARSER, "Parsing argument " + arguments.size());

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

    IdentifierExpression identifierExpression = new IdentifierExpression(
      tokenIdentifier.getValue(), tokenIdentifier, tokenIdentifier, tokenizer.getRawText()
    );

    FunctionInvocationExpression functionExpression = new FunctionInvocationExpression(
      identifierExpression, arguments, tokenMinus == null ? tokenIdentifier : tokenMinus, tk, tokenizer.getRawText()
    );

    // Wrap the function in a sign flipping expression
    if (flipResult)
      return new FlipSignExpression(functionExpression, tokenMinus, functionExpression.getTail(), tokenizer.getRawText());

    return functionExpression;
  }

  private AExpression parseNegationExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a negotiation expression");

    Token tk = tokenizer.peekToken();

    // There's no not operator as the next token, hand over to the next higher precedence parser
    if (tk == null || tk.getType() != TokenType.BOOL_NOT) {
      logger.logDebug( DebugLogLevel.PARSER, "Not a negotiation expression");
      return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    }

    // Consume the not operator
    Token notOperator = tokenizer.consumeToken();

    // Parse the following expression
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse the expression to negate");
    AExpression expression = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);

    // Return a wrapper on that expression which will negate it
    logger.logDebug( DebugLogLevel.PARSER, "Wrapping the parsed expression in order to negate it");
    return new InvertExpression(expression, notOperator, expression.getTail(), tokenizer.getRawText());
  }

  private AExpression parseComparisonExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a comparison expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

    while (
      (tk = tokenizer.peekToken()) != null &&
        (
          tk.getType() == TokenType.GREATER_THAN || tk.getType() == TokenType.GREATER_THAN_OR_EQUAL ||
          tk.getType() == TokenType.LESS_THAN || tk.getType() == TokenType.LESS_THAN_OR_EQUAL
        )
    ) {
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

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this comparison expression");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      lhs = new ComparisonExpression(lhs, rhs, operator, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private AExpression parseConcatenationExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a concatenation expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.CONCATENATE) {
      tokenizer.consumeToken();

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this concatenation expression");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      lhs = new ConcatenationExpression(lhs, rhs, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private AExpression parseDisjunctionExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a disjunction expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.BOOL_OR) {
      tokenizer.consumeToken();

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this disjunction expression");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      lhs = new DisjunctionExpression(lhs, rhs, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private AExpression parseConjunctionExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a conjunction expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer,  precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.BOOL_AND) {
      tokenizer.consumeToken();

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this conjunction expression");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer,  precedenceLadder, precedenceSelf);
      lhs = new ConjunctionExpression(lhs, rhs, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private AExpression parseEqualityExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a equality expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

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

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this equality expression");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      lhs = new EqualityExpression(lhs, rhs, operator, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private AExpression parseAdditiveExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a additive expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

    while (
      (tk = tokenizer.peekToken()) != null &&
      (tk.getType() == TokenType.PLUS || tk.getType() == TokenType.MINUS)
    ) {
      tokenizer.consumeToken();

      MathOperation operator = MathOperation.ADDITION;

      if (tk.getType() == TokenType.MINUS)
        operator = MathOperation.SUBTRACTION;

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this additive operation");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      lhs = new MathExpression(lhs, rhs, operator, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private AExpression parseMultiplicativeExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a multiplicative expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

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

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this multiplicative operation");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      lhs = new MathExpression(lhs, rhs, operator, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private @Nullable AExpression parseParenthesisExpression(ITokenizer tokenizer) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a parenthesis expression");

    Token tk = tokenizer.peekToken();

    // End reached
    if (tk == null)
      return null;

    Token firstToken = tk;
    boolean consumedFirstToken = false;

    // The notation -() will flip the resulting number's sign
    // The notation !() will negate the resulting boolean
    if (tk.getType() == TokenType.MINUS || tk.getType() == TokenType.BOOL_NOT) {
      tokenizer.saveState(true);

      tokenizer.consumeToken();
      consumedFirstToken = true;

      logger.logDebug( DebugLogLevel.PARSER, "Found and consumed a parentheses modifier token");

      tk = tokenizer.peekToken();
    }

    // Not a parenthesis expression
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {

      // Put back the consumed token which would have had an effect on these parentheses
      if (consumedFirstToken) {
        logger.logDebug( DebugLogLevel.PARSER, "Putting the modifier token back");
        tokenizer.restoreState(true);
      }

      logger.logDebug( DebugLogLevel.PARSER, "Not a parenthesis expression");
      return null;
    }

    // Don't need to return anymore as we're now inside parentheses
    if (consumedFirstToken)
      tokenizer.discardState(true);

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse the inner expression");

    // Parse the expression within the parentheses (start climbing the ladder all over again)
    AExpression expression = precedenceLadder[0].apply(tokenizer, precedenceLadder, 0);

    tk = tokenizer.consumeToken();

    // A previously opened parenthesis has to be closed again
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    logger.logDebug( DebugLogLevel.PARSER, "Validated the closing parenthesis");

    // Wrap the expression within a unary expression based on the first token's type
    switch (firstToken.getType()) {
      case MINUS:
        logger.logDebug( DebugLogLevel.PARSER, "Wrapping expression in order to flip it's sign");
        expression = new FlipSignExpression(expression, firstToken, expression.getTail(), tokenizer.getRawText());
        break;

      case BOOL_NOT:
        logger.logDebug( DebugLogLevel.PARSER, "Wrapping expression in order to invert it");
        expression = new InvertExpression(expression, firstToken, expression.getTail(), tokenizer.getRawText());
        break;
    }

    return expression;
  }

  private AExpression parseExponentiationExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a exponentiation expression");

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
    Token tk, head = lhs.getHead();

    while (
      (tk = tokenizer.peekToken()) != null &&
      tk.getType() == TokenType.EXPONENT
    ) {
      tokenizer.consumeToken();

      logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a rhs for this exponentiation operation");
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
      lhs = new MathExpression(lhs, rhs, MathOperation.POWER, head, rhs.getTail(), tokenizer.getRawText());
    }

    return lhs;
  }

  private AExpression parseExpression(ITokenizer tokenizer, FExpressionParser[] precedenceLadder, int precedenceSelf) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse an expression");
    AExpression expression;

    // Try to parse a callback expression before a parenthesis expression, as it's way easier
    // to check if it's actually a callback expression (only parens, identifiers, commas and the arrow)
    // It actually has the same precedence too, as it also resets precedence in it's body
    expression = this.parseCallbackExpression(tokenizer);
    if (expression != null)
      return expression;

    expression = this.parseParenthesisExpression(tokenizer);
    if (expression != null)
      return expression;

    expression = this.parseFunctionInvocationExpression(tokenizer);
    if (expression != null)
      return expression;

    // Just start climbing the precedence ladder and look for "normal" expressions
    return invokeNextPrecedenceParser(tokenizer, precedenceLadder, precedenceSelf);
  }

  private AExpression parsePrimaryExpression(ITokenizer tokenizer) throws AEvaluatorError {
    logger.logDebug( DebugLogLevel.PARSER, "Trying to parse a primary expression");

    Token tk = tokenizer.consumeToken();

    if (tk == null)
      throw new UnexpectedTokenError(tokenizer, null, TokenType.valueTypes);

    // Whether the primary expression has been marked as negative
    boolean isNegative = false;
    Token negativeToken = null;

    // Notation of a negative number
    if (tk.getType() == TokenType.MINUS) {
      tk = tokenizer.consumeToken();

      // Either no token left or it's not a number, a double or an identifier
      if (tk == null || !(tk.getType() == TokenType.LONG || tk.getType() == TokenType.DOUBLE || tk.getType() == TokenType.IDENTIFIER))
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.LONG, TokenType.DOUBLE, TokenType.IDENTIFIER);

      negativeToken = tk;
      isNegative = true;
    }

    Token head = negativeToken == null ? tk : negativeToken;

    switch (tk.getType()) {
      case LONG:
        logger.logDebug( DebugLogLevel.PARSER, "Found an integer");
        return new LongExpression((isNegative ? -1 : 1) * Integer.parseInt(tk.getValue()), head, tk, tokenizer.getRawText());

      case DOUBLE:
        logger.logDebug( DebugLogLevel.PARSER, "Found a double");
        return new DoubleExpression((isNegative ? -1 : 1) * Double.parseDouble(tk.getValue()), head, tk, tokenizer.getRawText());

      case STRING:
        logger.logDebug( DebugLogLevel.PARSER, "Found a string");
        return new StringExpression(tk.getValue(), head, tk, tokenizer.getRawText());

      case IDENTIFIER: {
        logger.logDebug( DebugLogLevel.PARSER, "Found an identifier");
        IdentifierExpression identifier = new IdentifierExpression(tk.getValue(), tk, tk, tokenizer.getRawText());
        return isNegative ? new FlipSignExpression(identifier, head, tk, tokenizer.getRawText()) : identifier;
      }

      case TRUE:
        logger.logDebug( DebugLogLevel.PARSER, "Found the true literal");
        return new LiteralExpression(LiteralType.TRUE, tk, tk, tokenizer.getRawText());

      case FALSE:
        logger.logDebug( DebugLogLevel.PARSER, "Found the false literal");
      return new LiteralExpression(LiteralType.FALSE, tk, tk, tokenizer.getRawText());

      case NULL:
        logger.logDebug( DebugLogLevel.PARSER, "Found the null literal");
      return new LiteralExpression(LiteralType.NULL, tk, tk, tokenizer.getRawText());

      default:
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.valueTypes);
    }
  }
}
