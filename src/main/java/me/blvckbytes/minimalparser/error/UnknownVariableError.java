package me.blvckbytes.minimalparser.error;

public class UnknownVariableError extends AParserError {

  public UnknownVariableError(int row, int col) {
    super(row, col, "Unknown variable encountered");
  }
}
