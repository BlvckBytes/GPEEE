package me.blvckbytes.minimalparser.error;

public class UnknownTokenError extends AParserError {

  public UnknownTokenError(int row, int col) {
    super(row, col, "Unknown token encountered");
  }
}
