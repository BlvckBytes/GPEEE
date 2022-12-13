package me.blvckbytes.minimalparser.parser.expression;

import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.MathOperation;

public class MathExpression extends BinaryExpression {

  private final MathOperation operation;

  public MathExpression(AExpression lhs, AExpression rhs, MathOperation operation) {
    super(lhs, rhs);
    this.operation = operation;
  }

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {

    // TODO: Add proper support for float values
    // TODO: Think ahead about what types external numbers might have...
    Integer lhsV = (Integer) evaluateExpression(lhs, context, valueInterpreter);
    Integer rhsV = (Integer) evaluateExpression(rhs, context, valueInterpreter);

    switch (operation) {
      case ADDITION:
        return lhsV + rhsV;

      case SUBTRACTION:
        return lhsV - rhsV;

      case MULTIPLICATION:
        return lhsV * rhsV;

      case DIVISION:
        return lhsV / rhsV;

      case MODULO:
        return lhsV % rhsV;

      case POWER:
        return (int) Math.pow(lhsV, rhsV);

      default:
        return 0;
    }
  }
}
