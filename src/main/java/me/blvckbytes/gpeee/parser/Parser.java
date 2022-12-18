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

import me.blvckbytes.gpeee.Tuple;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
      (tk, s) -> this.parsePrimaryExpression(tk),
    };
  }

  /**
   * Parses all available tokens into an abstract syntax tree (AST)
   * @return AST root ready for execution
   */
  public AExpression parse(ITokenizer tokenizer) throws AEvaluatorError {
    // Start to parse the lowest precedence expression and climb up
    AExpression result = invokeLowestPrecedenceParser(tokenizer);

    // If there are still tokens left after parsing an expression, the expression
    // wasn't closed in itself and has thus to be malformed, as this parser is only
    // intended for mono-expression "programs"
    Token tk = tokenizer.peekToken();
    if (tk != null)
      throw new UnexpectedTokenError(tokenizer, tk);

    return result;
  }

  //=========================================================================//
  //                            Expression Parsers                           //
  //=========================================================================//

  /////////////////////// Complex Expressions ///////////////////////

  private @Nullable AExpression parseCallbackExpression(ITokenizer tokenizer) throws AEvaluatorError {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a callback expression");
    //#endif

    Token tk = tokenizer.peekToken();

    // There's no opening parenthesis as the next token
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Not a callback expression");
      //#endif
      return null;
    }

    // Save once before consuming anything
    tokenizer.saveState(true);

    // Consume the opening parenthesis
    Token head = tokenizer.consumeToken();

    List<IdentifierExpression> signature = new ArrayList<>();

    // As long as there is no closing parenthesis, there are still arguments left
    while ((tk = tokenizer.peekToken()) != null && tk.getType() != TokenType.PARENTHESIS_CLOSE) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Parsing argument " + signature.size());
      //#endif

      if (signature.size() > 0) {
        // Arguments other than the first one need to be separated out by a comma
        // If there's no comma, this cannot be a callback expression and is more likely a parenthesis expression
        if (tk.getType() != TokenType.COMMA) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogLevel.PARSER, "Not a callback expression");
          //#endif
          tokenizer.restoreState(true);
          return null;
        }

        // Consume that comma
        tokenizer.consumeToken();
      }

      AExpression identifier = parsePrimaryExpression(tokenizer);

      // Anything else than an identifier cannot be within a callback's parentheses
      if (!(identifier instanceof IdentifierExpression)) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Not a callback expression");
        //#endif
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
    AExpression body = invokeLowestPrecedenceParser(tokenizer);

    return new CallbackExpression(signature, body, head, body.getTail(), tokenizer.getRawText());
  }

  private @Nullable AExpression parseIndexExpression(ITokenizer tokenizer) throws AEvaluatorError {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a index expression");
    //#endif

    Token tk = tokenizer.peekToken();

    // There's no identifier as the next token
    if (tk == null || tk.getType() != TokenType.IDENTIFIER) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Not a index expression");
      //#endif
      return null;
    }

    // Store before consuming the identifier
    tokenizer.saveState(true);

    // Consume the identifier
    Token tokenIdentifier = tk;
    tokenizer.consumeToken();

    tk = tokenizer.peekToken();

    // There's no opening bracket as the next token
    if (tk == null || tk.getType() != TokenType.BRACKET_OPEN) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Not a index expression");
      //#endif

      tokenizer.restoreState(true);
      return null;
    }

    // Not going to go need to restore anymore, this has to be a index expression
    tokenizer.discardState(true);

    // Consume the opening bracket
    tokenizer.consumeToken();

    // Parse the key expression
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Parsing the indexing key expression");
    //#endif
    AExpression key = invokeLowestPrecedenceParser(tokenizer);

    // Index expressions have to be terminated with a closing bracket
    tk = tokenizer.consumeToken();
    if (tk == null || tk.getType() != TokenType.BRACKET_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.BRACKET_CLOSE);

    IdentifierExpression identifierExpression = new IdentifierExpression(
      tokenIdentifier.getValue(), tokenIdentifier, tokenIdentifier, tokenizer.getRawText()
    );

    return new IndexExpression(
      identifierExpression, key, identifierExpression.getHead(), tk, tokenizer.getRawText()
    );
  }

  private @Nullable AExpression parseFunctionInvocationExpression(ITokenizer tokenizer) throws AEvaluatorError {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a function invocation expression");
    //#endif

    Token tk = tokenizer.peekToken();

    // There's no identifier or minus as the next token
    if (tk == null || !(tk.getType() == TokenType.IDENTIFIER || tk.getType() == TokenType.MINUS)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Not a function invocation expression");
      //#endif
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
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Not a function invocation expression");
        //#endif

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
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Not a function invocation expression");
      //#endif

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

    List<Tuple<AExpression, @Nullable IdentifierExpression>> arguments = new ArrayList<>();

    // As long as there is no closing parenthesis, there are still arguments left
    while ((tk = tokenizer.peekToken()) != null && tk.getType() != TokenType.PARENTHESIS_CLOSE) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Parsing argument " + arguments.size());
      //#endif

      if (arguments.size() > 0) {
        // Arguments other than the first one need to be separated out by a comma
        if (tk.getType() != TokenType.COMMA)
          throw new UnexpectedTokenError(tokenizer, tk, TokenType.COMMA);

        // Consume that comma
        tokenizer.consumeToken();
      }

      Token identifier = null;
      if ((tk = tokenizer.peekToken()) != null && tk.getType() == TokenType.IDENTIFIER) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a named argument");
        //#endif

        // Save before consuming so the next token can be peeked too
        tokenizer.saveState(true);
        identifier = tokenizer.consumeToken();

        // There's no assign token following this identifier, it cannot be a named argument
        if ((tk = tokenizer.peekToken()) == null || tk.getType() != TokenType.ASSIGN) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogLevel.PARSER, "Not a named argument");
          //#endif

          // Put the identifier back
          tokenizer.restoreState(true);
          identifier = null;
        }

        // Is a named argument
        else {
          // Don't need to revert anymore
          tokenizer.discardState(true);

          // Consume the assign token
          tokenizer.consumeToken();
        }
      }

      arguments.add(Tuple.of(
        // Parse the argument expression (start climbing the ladder all over again)
        invokeLowestPrecedenceParser(tokenizer),
        identifier == null ? null : new IdentifierExpression(identifier.getValue(), identifier, identifier, tokenizer.getRawText())
      ));
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

  private @Nullable AExpression parseParenthesisExpression(ITokenizer tokenizer) throws AEvaluatorError {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a parenthesis expression");
    //#endif

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

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Found and consumed a parentheses modifier token");
      //#endif

      tk = tokenizer.peekToken();
    }

    // Not a parenthesis expression
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {

      // Put back the consumed token which would have had an effect on these parentheses
      if (consumedFirstToken) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Putting the modifier token back");
        //#endif
        tokenizer.restoreState(true);
      }

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Not a parenthesis expression");
      //#endif
      return null;
    }

    // Don't need to return anymore as we're now inside parentheses
    if (consumedFirstToken)
      tokenizer.discardState(true);

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse the inner expression");
    //#endif

    // Parse the expression within the parentheses (start climbing the ladder all over again)
    AExpression expression = invokeLowestPrecedenceParser(tokenizer);

    tk = tokenizer.consumeToken();

    // A previously opened parenthesis has to be closed again
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Validated the closing parenthesis");
    //#endif

    // Wrap the expression within a unary expression based on the first token's type
    switch (firstToken.getType()) {
      case MINUS:
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Wrapping expression in order to flip it's sign");
        //#endif
        expression = new FlipSignExpression(expression, firstToken, expression.getTail(), tokenizer.getRawText());
        break;

      case BOOL_NOT:
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Wrapping expression in order to invert it");
        //#endif
        expression = new InvertExpression(expression, firstToken, expression.getTail(), tokenizer.getRawText());
        break;
    }

    return expression;
  }


  /////////////////////// Unary Expressions ///////////////////////

  private AExpression parseNegationExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseUnaryExpression((input, h, t, op) -> (
      new InvertExpression(input, h, t, tokenizer.getRawText())
    ), tokenizer, precedenceSelf, TokenType.BOOL_NOT);
  }

  /////////////////////// Binary Expressions ///////////////////////

  private AExpression parseComparisonExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> {

        ComparisonOperation operator;
        switch (op.getType()) {
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

        return new ComparisonExpression(lhs, rhs, operator, h, t, tokenizer.getRawText());
      },
      tokenizer, precedenceSelf,
      TokenType.GREATER_THAN, TokenType.GREATER_THAN_OR_EQUAL, TokenType.LESS_THAN, TokenType.LESS_THAN_OR_EQUAL
    );
  }

  private AExpression parseConcatenationExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new ConcatenationExpression(lhs, rhs, h, t, tokenizer.getRawText()),
      tokenizer, precedenceSelf,
      TokenType.CONCATENATE
    );
  }

  private AExpression parseDisjunctionExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new DisjunctionExpression(lhs, rhs, h, t, tokenizer.getRawText()),
      tokenizer, precedenceSelf,
      TokenType.BOOL_AND
    );
  }

  private AExpression parseConjunctionExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new ConjunctionExpression(lhs, rhs, h, t, tokenizer.getRawText()),
      tokenizer, precedenceSelf,
      TokenType.BOOL_OR
    );
  }

  private AExpression parseEqualityExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> {

        EqualityOperation operator;
        switch (op.getType()) {
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

        return new EqualityExpression(lhs, rhs, operator, h, t, tokenizer.getRawText());
      },
      tokenizer, precedenceSelf,
      TokenType.VALUE_EQUALS, TokenType.VALUE_NOT_EQUALS, TokenType.VALUE_EQUALS_EXACT, TokenType.VALUE_NOT_EQUALS_EXACT
    );
  }

  private AExpression parseAdditiveExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> {
        MathOperation operator = MathOperation.ADDITION;

        if (op.getType() == TokenType.MINUS)
          operator = MathOperation.SUBTRACTION;

        return new MathExpression(lhs, rhs, operator, h, t, tokenizer.getRawText());
      },
      tokenizer, precedenceSelf,
      TokenType.PLUS, TokenType.MINUS
    );
  }

  private AExpression parseMultiplicativeExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> {
        MathOperation operator = MathOperation.MULTIPLICATION;

        if (op.getType() == TokenType.DIVISION)
          operator = MathOperation.DIVISION;

        else if (op.getType() == TokenType.MODULO)
          operator = MathOperation.MODULO;

        return new MathExpression(lhs, rhs, operator, h, t, tokenizer.getRawText());
      },
      tokenizer, precedenceSelf,
      TokenType.MULTIPLICATION, TokenType.DIVISION, TokenType.MODULO
    );
  }

  private AExpression parseExponentiationExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new MathExpression(lhs, rhs, MathOperation.POWER, h, t, tokenizer.getRawText()),
      tokenizer, precedenceSelf,
      TokenType.EXPONENT
    );
  }

  //////////////////////// Main Expressions ////////////////////////

  private AExpression parseExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse an expression");
    //#endif
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

    expression = this.parseIndexExpression(tokenizer);
    if (expression != null)
      return expression;

    expression = this.parseFunctionInvocationExpression(tokenizer);
    if (expression != null)
      return expression;

    // Just start climbing the precedence ladder and look for "normal" expressions
    return invokeNextPrecedenceParser(tokenizer, precedenceSelf);
  }

  private AExpression parsePrimaryExpression(ITokenizer tokenizer) throws AEvaluatorError {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a primary expression");
    //#endif

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
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Found an integer");
        //#endif
        return new LongExpression((isNegative ? -1 : 1) * Integer.parseInt(tk.getValue()), head, tk, tokenizer.getRawText());

      case DOUBLE:
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Found a double");
        //#endif
        return new DoubleExpression((isNegative ? -1 : 1) * Double.parseDouble(tk.getValue()), head, tk, tokenizer.getRawText());

      case STRING:
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Found a string");
        //#endif
        return new StringExpression(tk.getValue(), head, tk, tokenizer.getRawText());

      case IDENTIFIER: {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Found an identifier");
        //#endif
        IdentifierExpression identifier = new IdentifierExpression(tk.getValue(), tk, tk, tokenizer.getRawText());
        return isNegative ? new FlipSignExpression(identifier, head, tk, tokenizer.getRawText()) : identifier;
      }

      case TRUE:
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Found the true literal");
        //#endif
        return new LiteralExpression(LiteralType.TRUE, tk, tk, tokenizer.getRawText());

      case FALSE:
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Found the false literal");
        //#endif
      return new LiteralExpression(LiteralType.FALSE, tk, tk, tokenizer.getRawText());

      case NULL:
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.PARSER, "Found the null literal");
        //#endif
      return new LiteralExpression(LiteralType.NULL, tk, tk, tokenizer.getRawText());

      default:
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.valueTypes);
    }
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Invokes the lowest expression parser within the sequence dictated by the precedence ladder
   * @param tokenizer Current parsing context's tokenizer reference
   * @return Result of invoking the lowest expression parser
   */
  private AExpression invokeLowestPrecedenceParser(ITokenizer tokenizer) throws AEvaluatorError {
    return precedenceLadder[0].apply(tokenizer, 0);
  }

  /**
   * Invokes the next expression parser within the sequence dictated by the precedence ladder
   * @param tokenizer Current parsing context's tokenizer reference
   * @param precedenceSelf Precedence of the current expression parser wanting to invoke the next
   * @return Result of invoking the next expression parser
   */
  private AExpression invokeNextPrecedenceParser(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return precedenceLadder[precedenceSelf + 1].apply(tokenizer, precedenceSelf + 1);
  }

  private boolean isOperator(TokenType[] operators, Token tk) {
    for (TokenType operator : operators) {
      if (tk.getType() == operator)
        return true;
    }
    return false;
  }

  private AExpression parseUnaryExpression(FUnaryExpressionWrapper wrapper, ITokenizer tokenizer, int precedenceSelf, TokenType... operators) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a unary expression for the operator " + Arrays.stream(operators).map(Enum::name).collect(Collectors.joining("|")));
    //#endif

    Token tk = tokenizer.peekToken();

    // There's no not operator as the next token, hand over to the next higher precedence parser
    if (tk == null || !isOperator(operators, tk)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Not a negotiation expression");
      //#endif
      return invokeNextPrecedenceParser(tokenizer, precedenceSelf);
    }

    // Consume the operator
    Token operator = tokenizer.consumeToken();

    // Parse the following expression
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse an input for this expression");
    //#endif
    AExpression expression = invokeNextPrecedenceParser(tokenizer, precedenceSelf);

    return wrapper.apply(expression, operator, expression.getTail(), operator);
  }

  private AExpression parseBinaryExpression(FBinaryExpressionWrapper wrapper, ITokenizer tokenizer, int precedenceSelf, TokenType... operators) throws AEvaluatorError {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a binary expression for the operator " + Arrays.stream(operators).map(Enum::name).collect(Collectors.joining("|")));
    //#endif

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceSelf);
    Token tk, head = lhs.getHead();

    while (
      (tk = tokenizer.peekToken()) != null &&
        isOperator(operators, tk)
    ) {
      // Consume the operator
      tokenizer.consumeToken();

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.PARSER, "Trying to parse a rhs for this operation");
      //#endif
      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceSelf);
      lhs = wrapper.apply(lhs, rhs, head, rhs.getTail(), tk);
    }

    return lhs;
  }
}
