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

package me.blvckbytes.gpeee.tokenizer;

import me.blvckbytes.gpeee.logging.DebugLogLevel;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.error.AEvaluatorError;
import me.blvckbytes.gpeee.error.UnknownTokenError;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class Tokenizer implements ITokenizer {

  private final String rawText;
  private final ILogger logger;
  private final char[] text;
  private final Stack<TokenizerState> saveStates;
  private TokenizerState state;

  public Tokenizer(ILogger logger, String text) {
    this.rawText = text;
    this.logger = logger;
    this.text = text.toCharArray();
    this.state = new TokenizerState();
    this.saveStates = new Stack<>();
  }

  //=========================================================================//
  //                                ITokenizer                               //
  //=========================================================================//

  @Override
  public String getRawText() {
    return rawText;
  }

  @Override
  public boolean hasNextChar() {
    return state.charIndex < this.text.length;
  }

  @Override
  public boolean isConsideredWhitespace(char c) {
    return c == ' ' || c == '\t';
  }

  @Override
  public void saveState(boolean debugLog) {
    this.saveStates.push(this.state.copy());

    //#if mvn.project.property.production != "true"
    if (debugLog)
      logger.logDebug(DebugLogLevel.TOKENIZER, "Saved state " + this.saveStates.size() + " (charIndex=" + state.charIndex + ")");
    //#endif
  }

  @Override
  public void restoreState(boolean debugLog) {
    int sizeBefore = this.saveStates.size();
    this.state = this.saveStates.pop();

    //#if mvn.project.property.production != "true"
    if (debugLog)
      logger.logDebug(DebugLogLevel.TOKENIZER, "Restored state " + sizeBefore + " (charIndex=" + state.charIndex + ")");
    //#endif
  }

  @Override
  public TokenizerState discardState(boolean debugLog) {
    int sizeBefore = this.saveStates.size();
    TokenizerState state = this.saveStates.pop();

    //#if mvn.project.property.production != "true"
    if (debugLog)
      logger.logDebug(DebugLogLevel.TOKENIZER, "Discarded state " + sizeBefore + " (charIndex=" + state.charIndex + ")");
    //#endif

    return state;
  }

  @Override
  public char nextChar() {
    char next = this.text[state.charIndex++];

    if (next == '\n') {
      ++state.row;
      state.colStack.push(state.col);
      state.col = 0;
    } else {
      ++state.col;
    }

    return next;
  }

  @Override
  public @Nullable Character previousChar() {
    return state.charIndex == 0 ? null : this.text[state.charIndex - 2];
  }

  @Override
  public char peekNextChar() {
    return this.text[state.charIndex];
  }

  @Override
  public void undoNextChar() {
    char lastChar = this.text[state.charIndex - 1];

    if (lastChar == '\n') {
      --state.row;
      state.col = state.colStack.pop();
    }

    else
      --state.col;

    state.charIndex--;
  }

  @Override
  public @Nullable Token peekToken() throws AEvaluatorError {
    eatComments();

    if (state.currentToken == null)
      readNextToken();

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.TOKENIZER, "Peeked token " + state.currentToken);
    //#endif

    return state.currentToken;
  }

  @Override
  public @Nullable Token consumeToken() throws AEvaluatorError {
    state.previousToken = state.currentToken;
    eatComments();

    if (state.currentToken == null)
      readNextToken();

    Token result = state.currentToken;
    readNextToken();

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogLevel.TOKENIZER, "Consumed token " + result);
    //#endif

    return result;
  }

  @Override
  public @Nullable Token previousToken() throws AEvaluatorError {
    return state.previousToken;
  }

  @Override
  public int getCurrentRow() {
    return state.row;
  }

  @Override
  public int getCurrentCol() {
    return state.col;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private void eatComments() {
    int c = 0;
    while (state.currentToken != null && state.currentToken.getType() == TokenType.COMMENT) {
      readNextToken();
      ++c;
    }

    //#if mvn.project.property.production != "true"
    if (c > 0)
      logger.logDebug(DebugLogLevel.TOKENIZER, "Ate " + c + " comment(s)");
    //#endif
  }

  private void eatWhitespace() {
    int ate = 0;

    while (hasNextChar() && (isConsideredWhitespace(peekNextChar()) || peekNextChar() == '\n')) {
      ++ate;
      nextChar();
    }

    //#if mvn.project.property.production != "true"
    if (ate > 0)
      logger.logDebug(DebugLogLevel.TOKENIZER, "Ate " + ate + " character(s) of whitespace");
    //#endif
  }

  /**
   * Reads the next token or null if nothing is available into the local state
   */
  private void readNextToken() throws AEvaluatorError {
    eatWhitespace();

    if (!hasNextChar()) {
      state.currentToken = null;
      return;
    }

    for (TokenType tryType : TokenType.valuesInTrialOrder) {
      FTokenReader reader = tryType.getTokenReader();

      // Token not yet implemented
      if (reader == null) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogLevel.TOKENIZER, "Token not yet implemented");
        //#endif
        continue;
      }

      saveState(false);

      String result = reader.apply(this);

      // This reader wasn't successful, restore and try the next in line
      if (result == null) {
        restoreState(false);
        continue;
      }

      // Discard the saved state (to move forwards) but use it as the token's row/col supplier
      TokenizerState previousState = discardState(false);
      state.currentToken = new Token(tryType, previousState.row, previousState.col, result);

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogLevel.TOKENIZER, "Reader for " + tryType + " was successful");
      //#endif
      return;
    }

    // No tokenizer matched
    throw new UnknownTokenError(state.row, state.col, rawText);
  }
}
