package me.blvckbytes.minimalparser.error;

public class UnterminatedStringError extends AParserError {

  public UnterminatedStringError(int row, int col) {
    super(row, col, "Strings need to start and end with \"");
  }
}
