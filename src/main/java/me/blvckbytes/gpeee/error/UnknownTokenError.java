package me.blvckbytes.gpeee.error;

public class UnknownTokenError extends AEvaluatorError {

  public UnknownTokenError(int row, int col, String rawInput) {
    super(row, col, "Unknown token encountered", rawInput);
  }
}
