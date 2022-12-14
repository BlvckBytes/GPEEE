package me.blvckbytes.gpeee.error;

import me.blvckbytes.gpeee.parser.expression.AExpression;

public class UndefinedVariableError extends AEvaluatorError {

  public UndefinedVariableError(AExpression identifier) {
    super(
      identifier.getHead().getRow(),
      identifier.getHead().getCol(),
      identifier.getFullContainingExpression(),
      "This variable identifier has not been defined within the current environment"
    );
  }
}
