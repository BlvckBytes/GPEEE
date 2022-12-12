package me.blvckbytes.minimalparser.error;

public class UnknownLookupError extends AParserError {

  public UnknownLookupError(int row, int col) {
    super(row, col, "Unknown lookup encountered");
  }
}
