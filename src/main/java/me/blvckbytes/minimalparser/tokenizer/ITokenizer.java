package me.blvckbytes.minimalparser.tokenizer;

import me.blvckbytes.minimalparser.error.AParserError;
import org.jetbrains.annotations.Nullable;

public interface ITokenizer {

  void undoNextChar();

  char nextChar();

  @Nullable Character previousChar();

  char peekNextChar();

  boolean hasNextChar();

  boolean isConsideredWhitespace(char c);

  void saveState();

  void restoreState();

  @Nullable Token peekToken() throws AParserError;
  @Nullable Token consumeToken() throws AParserError;

  int getCurrentRow();

  int getCurrentCol();

  String getRawText();

}
