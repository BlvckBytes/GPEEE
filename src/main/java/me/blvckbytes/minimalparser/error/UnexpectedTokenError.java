package me.blvckbytes.minimalparser.error;

import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UnexpectedTokenError extends AParserError {

  public UnexpectedTokenError(int row, int col, @Nullable TokenType actual, TokenType... expected) {
    super(
      row, col,
      "Expected token " + formatTokenNames(expected) + ", found " + (actual == null ? "nothing" : actual.name()));
  }

  private static String formatTokenNames(TokenType[] tokens) {
    if (tokens.length == 0)
      return "EOF";

    return Arrays.stream(tokens)
      .map(TokenType::name)
      .collect(Collectors.joining("|"));
  }
}
