package me.blvckbytes.gpeee.error;

public class UnterminatedStringError extends AParserError {

  public UnterminatedStringError(int row, int col, String rawInput) {
    super(row, col, "Strings need to start and end with \"", rawInput);
  }
}
