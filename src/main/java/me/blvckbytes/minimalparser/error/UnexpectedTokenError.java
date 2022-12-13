package me.blvckbytes.minimalparser.error;

import me.blvckbytes.minimalparser.tokenizer.ITokenizer;
import me.blvckbytes.minimalparser.tokenizer.Token;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UnexpectedTokenError extends AParserError {

  public UnexpectedTokenError(ITokenizer tokenizer, @Nullable Token actual, TokenType... expected) {
    super(
      actual == null ? tokenizer.getCurrentRow() : actual.getRow(),
      actual == null ? tokenizer.getCurrentCol() : actual.getCol(),
      "Expected token " + formatTokenNames(expected) + ", found " + (actual == null ? "nothing" : actual.getType().name()));
  }

  private static String formatTokenNames(TokenType[] tokens) {
    if (tokens.length == 0)
      return "EOF";

    return Arrays.stream(tokens)
      .map(TokenType::name)
      .collect(Collectors.joining("|"));
  }
}
