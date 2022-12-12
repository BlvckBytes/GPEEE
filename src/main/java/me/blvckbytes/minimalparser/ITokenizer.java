package me.blvckbytes.minimalparser;

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

}
