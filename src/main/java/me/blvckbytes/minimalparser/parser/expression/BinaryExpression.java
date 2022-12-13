package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.parser.MathOperation;

@Getter
@AllArgsConstructor
public class BinaryExpression extends AExpression {

  private final AExpression lhs;
  private final AExpression rhs;
  private final MathOperation operation;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {

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
