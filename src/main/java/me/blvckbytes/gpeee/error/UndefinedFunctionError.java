package me.blvckbytes.gpeee.error;

import me.blvckbytes.gpeee.parser.expression.AExpression;

public class UndefinedFunctionError extends AEvaluatorError {

  public UndefinedFunctionError(AExpression identifier) {
    super(
      identifier.getHead().getRow(),
      identifier.getHead().getCol(),
      identifier.getFullContainingExpression(),
      "This function identifier has not been defined within the current environment"
    );
  }
}
