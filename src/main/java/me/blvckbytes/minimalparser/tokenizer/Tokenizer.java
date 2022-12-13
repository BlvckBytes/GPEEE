package me.blvckbytes.minimalparser.tokenizer;

import lombok.Getter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.error.UnknownTokenError;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class Tokenizer implements ITokenizer {

  @Getter
  private final String rawText;

  private final char[] text;
  private final Stack<TokenizerState> saveStates;
  private TokenizerState state;
  private Token currentToken;

  public Tokenizer(String text) {
    this.rawText = text;
    this.text = text.toCharArray();
    this.state = new TokenizerState();
    this.saveStates = new Stack<>();
  }

  public boolean hasNextChar() {
    return state.charIndex < this.text.length;
  }

  @Override
  public boolean isConsideredWhitespace(char c) {
    return c == ' ' || c == '\t';
  }

  @Override
  public void saveState() {
    this.saveStates.push(this.state.copy());
  }

  @Override
  public void restoreState() {
    this.state = this.saveStates.pop();
  }

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

  private void eatWhitespace() {
    while (hasNextChar() && (isConsideredWhitespace(peekNextChar()) || peekNextChar() == '\n'))
      nextChar();
  }

  @Override
  public @Nullable Token peekToken() throws AParserError {
    if (currentToken == null)
      currentToken = readNextToken();

    return currentToken;
  }

  public @Nullable Token consumeToken() throws AParserError {
    if (currentToken == null)
      currentToken = readNextToken();

    Token result = currentToken;
    currentToken = readNextToken();

    return result;
  }

  @Override
  public int getCurrentRow() {
    return state.row;
  }

  @Override
  public int getCurrentCol() {
    return state.col;
  }

  private @Nullable Token readNextToken() throws AParserError {
    eatWhitespace();

    if (!hasNextChar())
      return null;

    for (TokenType tryType : TokenType.valuesInTrialOrder) {
      FTokenReader reader = tryType.getTokenReader();

      // Token not yet implemented
      if (reader == null)
        continue;

      saveState();

      String result = reader.apply(this);

      // This reader wasn't successful, restore and try the next in line
      if (result == null) {
        restoreState();
        continue;
      }

      TokenizerState previousState = saveStates.pop();
      return new Token(tryType, previousState.row, previousState.col, result);
    }

    // No tokenizer matched
    throw new UnknownTokenError(state.row, state.col);
  }
}
