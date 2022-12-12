package me.blvckbytes.minimalparser.error;

public class UnknownFunctionError extends AParserError {

  public UnknownFunctionError(int row, int col) {
    super(row, col, "Unknown function encountered");
  }
}
