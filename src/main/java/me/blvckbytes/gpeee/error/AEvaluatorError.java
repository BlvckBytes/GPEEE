package me.blvckbytes.gpeee.error;

import lombok.Getter;

@Getter
public abstract class AEvaluatorError extends RuntimeException {

  public AEvaluatorError(int row, int col, String rawInput, String text) {
    super(generateWarning(row, col, rawInput, text));
  }

  private static String generateWarning(int row, int col, String rawInput, String text) {
    StringBuilder result = new StringBuilder();
    String targetLine = rawInput.split("\n")[row];

    String lineNumber = (row + 1) + ": ";

    result
      // Show the target line with it's line number
      .append(lineNumber).append(targetLine).append("\n")
      // Draw an indicator under the target column and print the text next to it
      .append(" ".repeat(col + lineNumber.length())).append("^").append(" ").append(text).append("\n");

    return result.toString();
  }
}
