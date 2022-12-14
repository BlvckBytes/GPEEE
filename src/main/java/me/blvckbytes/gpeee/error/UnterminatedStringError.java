package me.blvckbytes.gpeee.error;

public class UnterminatedStringError extends AEvaluatorError {

  public UnterminatedStringError(int row, int col, String rawInput) {
    super(row, col, "Strings need to start and end with \"", rawInput);
  }
}
