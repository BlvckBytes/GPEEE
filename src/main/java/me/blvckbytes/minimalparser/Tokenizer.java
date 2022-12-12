package me.blvckbytes.minimalparser;

import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class Tokenizer implements ITokenizer {

  private final char[] text;
  private final Stack<TokenizerState> saveStates;
  private TokenizerState state;

  public Tokenizer(String text) {
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

  public @Nullable Token nextToken() {
    eatWhitespace();

    if (!hasNextChar())
      return null;

    for (TokenType tryType : TokenType.values) {
      FTokenReader reader = tryType.getTokenReader();

      // Token not yet implemented
      if (reader == null)
        continue;

      // Save tokenizer state
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
    // TODO: Throw an error!
    System.err.println("Nothing matched!");
    return null;
  }
}
