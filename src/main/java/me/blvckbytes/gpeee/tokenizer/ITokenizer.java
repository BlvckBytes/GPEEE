package me.blvckbytes.gpeee.tokenizer;

import me.blvckbytes.gpeee.error.AParserError;
import org.jetbrains.annotations.Nullable;

public interface ITokenizer {

  void undoNextChar();

  char nextChar();

  @Nullable Character previousChar();

  char peekNextChar();

  boolean hasNextChar();

  boolean isConsideredWhitespace(char c);

  void saveState(boolean debugLog);

  void restoreState(boolean debugLog);

  TokenizerState discardState(boolean debugLog);

  @Nullable Token peekToken() throws AParserError;
  @Nullable Token consumeToken() throws AParserError;

  int getCurrentRow();

  int getCurrentCol();

  String getRawText();

}
