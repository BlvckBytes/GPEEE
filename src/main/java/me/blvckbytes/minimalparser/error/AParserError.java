package me.blvckbytes.minimalparser.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class AParserError extends RuntimeException {

  private final int row, col;
  private final String text;

  public String generateWarning(String parsedText) {
    StringBuilder result = new StringBuilder();
    String targetLine = parsedText.split("\n")[row];

    String lineNumber = (row + 1) + ": ";

    result
      // Show the target line with it's line number
      .append(lineNumber).append(targetLine).append("\n")
      // Draw an indicator under the target column and print the text next to it
      .append(" ".repeat(col + lineNumber.length())).append("^").append(" ").append(text).append("\n");

    return result.toString();
  }
}
