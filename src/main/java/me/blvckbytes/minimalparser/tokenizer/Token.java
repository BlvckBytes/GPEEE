package me.blvckbytes.minimalparser;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Token {

  private final TokenType type;
  private final int row, col;
  private final String value;

  @Override
  public String toString() {
    return "Token{" +
      "type=" + type + " (" + type.getCategory() + ")" +
      ", row=" + row +
      ", col=" + col +
      ", value='" + value + '\'' +
      '}';
  }
}
